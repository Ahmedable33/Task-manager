import 'package:dio/dio.dart';
import 'package:shared_preferences/shared_preferences.dart';

class ApiService {
  final Dio dio = Dio(BaseOptions(
      baseUrl: const String.fromEnvironment('API_URL',
          defaultValue: 'http://10.0.2.2:8081')));
  Future<void> login(String email, String password) async {
    final response = await dio
        .post('/api/auth/login', data: {'email': email, 'password': password});
    await _saveToken(response.data['token'] as String);
  }

  Future<void> register(String name, String email, String password) async {
    final response = await dio.post('/api/auth/register',
        data: {'name': name, 'email': email, 'password': password});
    await _saveToken(response.data['token'] as String);
  }

  Future<List<Map<String, dynamic>>> tasks() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    final response = await dio.get('/api/tasks',
        options: Options(headers: {'Authorization': 'Bearer $token'}));
    return (response.data as List)
        .map((task) => Map<String, dynamic>.from(task as Map))
        .toList();
  }

  Future<Map<String, dynamic>> createTask(
      String title, String description, String status) async {
    final response = await dio.post('/api/tasks',
        data: {'title': title, 'description': description, 'status': status},
        options: Options(headers: {'Authorization': 'Bearer ${await token}'}));
    return Map<String, dynamic>.from(response.data as Map);
  }

  Future<Map<String, dynamic>> updateTask(
      int id, String title, String description, String status) async {
    final response = await dio.put('/api/tasks/$id',
        data: {'title': title, 'description': description, 'status': status},
        options: Options(headers: {'Authorization': 'Bearer ${await token}'}));
    return Map<String, dynamic>.from(response.data as Map);
  }

  Future<void> deleteTask(int id) async {
    await dio.delete('/api/tasks/$id',
        options: Options(headers: {'Authorization': 'Bearer ${await token}'}));
  }

  Future<void> logout() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('token');
  }

  Future<String?> get token async {
    final prefs = await SharedPreferences.getInstance();
    return prefs.getString('token');
  }

  Future<void> _saveToken(String value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('token', value);
  }
}
