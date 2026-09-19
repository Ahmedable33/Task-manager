import React, { useEffect, useMemo, useState } from 'react';
import { Check, ClipboardList, Clock3, LogOut, Plus, Search, Trash2, X } from 'lucide-react';
import { createRoot } from 'react-dom/client';
import toast, { Toaster } from 'react-hot-toast';
import { api } from './api';
import type { Status, Task } from './types';
import './styles.css';

const labels: Record<Status, string> = { TODO: 'À faire', IN_PROGRESS: 'En cours', DONE: 'Terminée' };
const colors: Record<Status, string> = { TODO: 'coral', IN_PROGRESS: 'gold', DONE: 'mint' };
const emptyTask = { title: '', description: '', status: 'TODO' as Status };

function App() {
  const [token, setToken] = useState(localStorage.getItem('token'));
  const [displayName, setDisplayName] = useState(localStorage.getItem('name') || localStorage.getItem('email') || '');
  const [loginMode, setLoginMode] = useState(true);
  const [authForm, setAuthForm] = useState({ name: '', email: '', password: '' });
  const [tasks, setTasks] = useState<Task[]>([]);
  const [taskForm, setTaskForm] = useState(emptyTask);
  const [query, setQuery] = useState('');
  const [filter, setFilter] = useState<'ALL' | Status>('ALL');
  const [editing, setEditing] = useState<number | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (token) api.get<Task[]>('/api/tasks').then(response => setTasks(response.data)).catch(logout);
  }, [token]);

  const visible = useMemo(() => tasks.filter(task =>
    (filter === 'ALL' || task.status === filter) &&
    `${task.title} ${task.description}`.toLowerCase().includes(query.toLowerCase())), [tasks, filter, query]);

  function logout() {
    localStorage.clear();
    setToken(null);
    setTasks([]);
  }

  async function authenticate(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    try {
      const payload = loginMode
        ? { email: authForm.email.trim(), password: authForm.password }
        : { name: authForm.name.trim(), email: authForm.email.trim(), password: authForm.password };
      const response = await api.post(`/api/auth/${loginMode ? 'login' : 'register'}`, payload);
      localStorage.setItem('token', response.data.token);
      localStorage.setItem('email', response.data.email);
      localStorage.setItem('name', response.data.name);
      setToken(response.data.token);
      setDisplayName(response.data.name);
      toast.success(loginMode ? 'Bon retour.' : 'Compte créé.');
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Vérifiez vos identifiants.');
    } finally {
      setLoading(false);
    }
  }

  async function saveTask(event: React.FormEvent) {
    event.preventDefault();
    if (!taskForm.title.trim()) return;
    try {
      const response = editing ? await api.put(`/api/tasks/${editing}`, taskForm) : await api.post('/api/tasks', taskForm);
      setTasks(current => editing ? current.map(task => task.id === editing ? response.data : task) : [response.data, ...current]);
      setTaskForm(emptyTask);
      setEditing(null);
      toast.success(editing ? 'Tâche mise à jour.' : 'Tâche créée.');
    } catch { toast.error('Impossible d’enregistrer la tâche.'); }
  }

  async function removeTask(id: number) {
    try {
      await api.delete(`/api/tasks/${id}`);
      setTasks(current => current.filter(task => task.id !== id));
      toast.success('Tâche supprimée.');
    } catch { toast.error('Suppression impossible.'); }
  }

  if (!token) return <><Toaster position="top-right" /><main className="auth-shell min-h-screen">
    <section className="auth-copy"><div className="brand"><span className="brand-mark"><ClipboardList size={22} /></span> Task Manager</div><p className="eyebrow">Votre espace de travail, enfin lisible.</p><h1>Les bonnes tâches.<br /><em>Au bon moment.</em></h1><p className="intro">Capturez ce qui compte, gardez le cap et avancez sans bruit.</p><div className="signal"><span>01</span><div><strong>Simple par défaut</strong><small>Un tableau qui suit votre rythme.</small></div></div></section>
    <section className="auth-card rounded-2xl"><div className="auth-tabs"><button className={loginMode ? 'active' : ''} onClick={() => setLoginMode(true)}>Connexion</button><button className={!loginMode ? 'active' : ''} onClick={() => setLoginMode(false)}>Inscription</button></div><h2>{loginMode ? 'Ravi de vous revoir.' : 'Commencez ici.'}</h2><p className="muted">{loginMode ? 'Retrouvez votre espace de travail.' : 'Un compte, trois listes, zéro friction.'}</p><form onSubmit={authenticate}>{!loginMode && <label>Nom<input type="text" required minLength={2} value={authForm.name} onChange={event => setAuthForm({ ...authForm, name: event.target.value })} placeholder="Votre nom" /></label>}<label>Email<input type="email" required value={authForm.email} onChange={event => setAuthForm({ ...authForm, email: event.target.value })} placeholder="vous@exemple.com" /></label><label>Mot de passe<input type="password" required minLength={8} value={authForm.password} onChange={event => setAuthForm({ ...authForm, password: event.target.value })} placeholder="8 caractères minimum" /></label><button className="primary wide rounded-lg" disabled={loading}>{loading ? 'Patientez...' : loginMode ? 'Entrer dans Task Manager' : 'Créer mon espace'}<span>→</span></button></form></section>
  </main></>;

  return <><Toaster position="top-right" /><div className="app-shell"><header><div className="brand"><span className="brand-mark"><ClipboardList size={22} /></span> Task Manager</div><div className="header-user"><span>{displayName}</span><button className="icon-button" title="Se déconnecter" onClick={logout}><LogOut size={18} /></button></div></header><main className="dashboard"><div className="dashboard-heading"><div><p className="eyebrow">Votre espace personnel</p><h1>Qu’est-ce qui compte aujourd’hui ?</h1><p className="muted">Une petite action après l’autre.</p></div><button className="primary" onClick={() => { setEditing(null); setTaskForm(emptyTask); document.getElementById('task-title')?.focus(); }}><Plus size={18} /> Nouvelle tâche</button></div><section className="stats"><div><small>Total</small><strong>{tasks.length.toString().padStart(2, '0')}</strong></div><div><small>En cours</small><strong>{tasks.filter(task => task.status === 'IN_PROGRESS').length.toString().padStart(2, '0')}</strong></div><div><small>Terminées</small><strong>{tasks.filter(task => task.status === 'DONE').length.toString().padStart(2, '0')}</strong></div></section><div className="workspace"><section className="task-panel"><div className="toolbar"><div className="search"><Search size={17} /><input placeholder="Rechercher une tâche..." value={query} onChange={event => setQuery(event.target.value)} /></div><div className="filters">{(['ALL', 'TODO', 'IN_PROGRESS', 'DONE'] as const).map(key => <button key={key} className={filter === key ? 'selected' : ''} onClick={() => setFilter(key)}>{key === 'ALL' ? 'Toutes' : labels[key]}</button>)}</div></div><div className="task-list">{visible.length ? visible.map(task => <article className="task-row" key={task.id}><button className={`check ${task.status === 'DONE' ? 'checked' : ''}`} title="Changer le statut" onClick={() => { const status = task.status === 'DONE' ? 'TODO' : 'DONE'; api.put(`/api/tasks/${task.id}`, { ...task, status }).then(response => setTasks(current => current.map(item => item.id === task.id ? response.data : item))); }}><Check size={15} /></button><div className="task-main"><strong>{task.title}</strong><p>{task.description || 'Sans description'}</p></div><span className={`status ${colors[task.status]}`}>{labels[task.status]}</span><button className="icon-button" title="Modifier" onClick={() => { setEditing(task.id); setTaskForm({ title: task.title, description: task.description || '', status: task.status }); }}><Clock3 size={16} /></button><button className="icon-button danger" title="Supprimer" onClick={() => removeTask(task.id)}><Trash2 size={16} /></button></article>) : <div className="empty"><h3>Aucune tâche ici.</h3><p>Ajoutez une idée et donnez-lui une place.</p></div>}</div></section><aside className="composer"><div className="composer-top"><span className="eyebrow">{editing ? 'MODIFICATION' : 'À VENIR'}</span>{editing && <button className="icon-button" title="Annuler" onClick={() => { setEditing(null); setTaskForm(emptyTask); }}><X size={18} /></button>}</div><h2>Une idée en tête ?</h2><form onSubmit={saveTask}><label>Titre<input id="task-title" required value={taskForm.title} onChange={event => setTaskForm({ ...taskForm, title: event.target.value })} /></label><label>Description<textarea rows={5} value={taskForm.description} onChange={event => setTaskForm({ ...taskForm, description: event.target.value })} /></label><label>Statut<select value={taskForm.status} onChange={event => setTaskForm({ ...taskForm, status: event.target.value as Status })}>{Object.entries(labels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}</select></label><button className="primary wide">{editing ? 'Enregistrer les changements' : 'Ajouter la tâche'}<Plus size={17} /></button></form></aside></div></main></div></>;
}

createRoot(document.getElementById('root')!).render(<React.StrictMode><App /></React.StrictMode>);
