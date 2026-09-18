import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'services/api_service.dart';

void main() => runApp(const TaskManagerApp());

class TaskManagerApp extends StatelessWidget {
  const TaskManagerApp({super.key});

  @override
  Widget build(BuildContext context) => MaterialApp(
        title: 'Task Manager',
        theme: ThemeData(
          colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff1e2925)),
          useMaterial3: true,
        ),
        home: const LoginScreen(),
      );
}

class LoginScreen extends StatefulWidget {
  const LoginScreen({super.key});

  @override
  State<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends State<LoginScreen> {
  final api = ApiService();
  final name = TextEditingController();
  final email = TextEditingController();
  final password = TextEditingController();
  bool registerMode = false;
  bool loading = false;

  @override
  void dispose() {
    name.dispose();
    email.dispose();
    password.dispose();
    super.dispose();
  }

  Future<void> submit() async {
    if ((registerMode && name.text.trim().length < 2) ||
        email.text.trim().isEmpty ||
        password.text.length < 8) {
      showMessage('Saisissez un email et un mot de passe de 8 caractères.');
      return;
    }
    setState(() => loading = true);
    try {
      if (registerMode) {
        await api.register(name.text.trim(), email.text.trim(), password.text);
      } else {
        await api.login(email.text.trim(), password.text);
      }
      if (mounted) {
        Navigator.pushReplacement(
            context, MaterialPageRoute(builder: (_) => const TaskListScreen()));
      }
    } on DioException catch (error) {
      showMessage(error.response?.data?['message']?.toString() ??
          'Impossible de contacter le serveur.');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  void showMessage(String message) {
    if (mounted) {
      ScaffoldMessenger.of(context)
          .showSnackBar(SnackBar(content: Text(message)));
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(28),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 420),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text('Task Manager',
                      textAlign: TextAlign.center,
                      style:
                          TextStyle(fontSize: 32, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  Text(registerMode ? 'Créer un compte' : 'Bon retour',
                      textAlign: TextAlign.center),
                  const SizedBox(height: 36),
                  if (registerMode)
                    TextField(
                        controller: name,
                        decoration: const InputDecoration(labelText: 'Nom')),
                  if (registerMode) const SizedBox(height: 16),
                  TextField(
                      controller: email,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(labelText: 'Email')),
                  const SizedBox(height: 16),
                  TextField(
                      controller: password,
                      obscureText: true,
                      decoration:
                          const InputDecoration(labelText: 'Mot de passe')),
                  const SizedBox(height: 28),
                  FilledButton(
                      onPressed: loading ? null : submit,
                      child: Text(loading
                          ? 'Patientez...'
                          : registerMode
                              ? 'Créer le compte'
                              : 'Se connecter')),
                  TextButton(
                      onPressed: loading
                          ? null
                          : () => setState(() => registerMode = !registerMode),
                      child: Text(registerMode
                          ? 'J’ai déjà un compte'
                          : 'Créer un compte')),
                ],
              ),
            ),
          ),
        ),
      );
}

class TaskListScreen extends StatefulWidget {
  const TaskListScreen({super.key});

  @override
  State<TaskListScreen> createState() => _TaskListScreenState();
}

class _TaskListScreenState extends State<TaskListScreen> {
  final api = ApiService();
  List<Map<String, dynamic>> tasks = [];
  bool loading = true;

  @override
  void initState() {
    super.initState();
    refresh();
  }

  Future<void> refresh() async {
    setState(() => loading = true);
    try {
      tasks = await api.tasks();
    } on DioException {
      showMessage('Impossible de charger les tâches.');
    } finally {
      if (mounted) setState(() => loading = false);
    }
  }

  Future<void> openEditor([Map<String, dynamic>? task]) async {
    final result = await showDialog<Map<String, String>>(
        context: context, builder: (_) => TaskDialog(task: task));
    if (result == null) return;
    try {
      final saved = task == null
          ? await api.createTask(
              result['title']!, result['description']!, result['status']!)
          : await api.updateTask(int.parse(task['id'].toString()),
              result['title']!, result['description']!, result['status']!);
      setState(() {
        if (task == null) {
          tasks = [saved, ...tasks];
        } else {
          final index = tasks.indexOf(task);
          tasks[index] = saved;
        }
      });
    } on DioException {
      showMessage('Impossible d’enregistrer la tâche.');
    }
  }

  Future<void> removeTask(Map<String, dynamic> task) async {
    try {
      await api.deleteTask(int.parse(task['id'].toString()));
      setState(() => tasks.remove(task));
    } on DioException {
      showMessage('Impossible de supprimer la tâche.');
    }
  }

  Future<void> logout() async {
    await api.logout();
    if (mounted) {
      Navigator.pushAndRemoveUntil(context,
          MaterialPageRoute(builder: (_) => const LoginScreen()), (_) => false);
    }
  }

  void showMessage(String message) => ScaffoldMessenger.of(context)
      .showSnackBar(SnackBar(content: Text(message)));

  @override
  Widget build(BuildContext context) => Scaffold(
        appBar: AppBar(title: const Text('Mes tâches'), actions: [
          IconButton(onPressed: refresh, icon: const Icon(Icons.refresh)),
          IconButton(onPressed: logout, icon: const Icon(Icons.logout)),
        ]),
        floatingActionButton: FloatingActionButton.extended(
            onPressed: () => openEditor(),
            icon: const Icon(Icons.add),
            label: const Text('Nouvelle tâche')),
        body: loading
            ? const Center(child: CircularProgressIndicator())
            : RefreshIndicator(
                onRefresh: refresh,
                child: tasks.isEmpty
                    ? ListView(children: const [
                        SizedBox(height: 180),
                        Center(child: Text('Aucune tâche pour le moment.'))
                      ])
                    : ListView.builder(
                        padding: const EdgeInsets.fromLTRB(12, 12, 12, 90),
                        itemCount: tasks.length,
                        itemBuilder: (_, index) {
                          final task = tasks[index];
                          return Card(
                            child: ListTile(
                              title: Text(task['title'].toString()),
                              subtitle: Text(task['description']?.toString() ??
                                  'Sans description'),
                              isThreeLine: true,
                              leading: Icon(statusIcon(task['status'])),
                              onTap: () => openEditor(task),
                              trailing: IconButton(
                                  onPressed: () => removeTask(task),
                                  icon: const Icon(Icons.delete_outline)),
                            ),
                          );
                        },
                      ),
              ),
      );

  IconData statusIcon(dynamic status) {
    switch (status) {
      case 'DONE':
        return Icons.check_circle;
      case 'IN_PROGRESS':
        return Icons.timelapse;
      default:
        return Icons.radio_button_unchecked;
    }
  }
}

class TaskDialog extends StatefulWidget {
  const TaskDialog({super.key, this.task});
  final Map<String, dynamic>? task;

  @override
  State<TaskDialog> createState() => _TaskDialogState();
}

class _TaskDialogState extends State<TaskDialog> {
  late final TextEditingController title;
  late final TextEditingController description;
  late String status;

  @override
  void initState() {
    super.initState();
    title = TextEditingController(text: widget.task?['title']?.toString());
    description =
        TextEditingController(text: widget.task?['description']?.toString());
    status = widget.task?['status']?.toString() ?? 'TODO';
  }

  @override
  void dispose() {
    title.dispose();
    description.dispose();
    super.dispose();
  }

  void save() {
    if (title.text.trim().isEmpty) return;
    Navigator.pop(context, {
      'title': title.text.trim(),
      'description': description.text.trim(),
      'status': status,
    });
  }

  @override
  Widget build(BuildContext context) => AlertDialog(
        title:
            Text(widget.task == null ? 'Nouvelle tâche' : 'Modifier la tâche'),
        content: SingleChildScrollView(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            TextField(
                controller: title,
                autofocus: true,
                decoration: const InputDecoration(labelText: 'Titre')),
            TextField(
                controller: description,
                maxLines: 3,
                decoration: const InputDecoration(labelText: 'Description')),
            DropdownButtonFormField<String>(
              initialValue: status,
              decoration: const InputDecoration(labelText: 'Statut'),
              items: const [
                DropdownMenuItem(value: 'TODO', child: Text('À faire')),
                DropdownMenuItem(value: 'IN_PROGRESS', child: Text('En cours')),
                DropdownMenuItem(value: 'DONE', child: Text('Terminée')),
              ],
              onChanged: (value) => setState(() => status = value ?? 'TODO'),
            ),
          ]),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.pop(context),
              child: const Text('Annuler')),
          FilledButton(onPressed: save, child: const Text('Enregistrer')),
        ],
      );
}
