"use client";

import { FormEvent, useEffect, useState } from "react";
import Navbar from "@/components/Navbar";
import { api } from "@/lib/api";
import type { UserResponse, UserRole } from "@/lib/types";

const roleLabels: Record<UserRole, string> = {
  ADMIN: "Administrador",
  USER: "Usuario",
};

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    role: "USER" as UserRole,
  });

  const loadUsers = () => {
    setLoading(true);
    api.users
      .listar()
      .then(setUsuarios)
      .catch((e) => setError(e instanceof Error ? e.message : "Erro ao carregar usuarios"))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadUsers();
  }, []);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    try {
      await api.users.criar(form);
      setForm({ name: "", email: "", password: "", role: "USER" });
      loadUsers();
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erro ao criar usuario");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDeactivate = async (id: string) => {
    if (!confirm("Desativar este usuario?")) return;
    await api.users.desativar(id);
    loadUsers();
  };

  return (
    <>
      <Navbar />
      <main className="max-w-6xl mx-auto px-6 py-8">
        <div className="mb-8">
          <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Usuarios</h1>
          <p className="text-sm text-slate-500 mt-1">Crie acessos e gerencie quem usa a plataforma</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-[380px_1fr] gap-6">
          <form onSubmit={handleSubmit} className="bg-white border border-slate-200 rounded-2xl p-6 shadow-sm h-fit">
            <h2 className="text-base font-semibold text-slate-700 mb-5">Novo usuario</h2>

            <label className="block text-sm font-medium text-slate-600 mb-1.5">Nome</label>
            <input
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              required
              maxLength={120}
              className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
            />

            <label className="block text-sm font-medium text-slate-600 mt-4 mb-1.5">E-mail</label>
            <input
              type="email"
              value={form.email}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
              className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
            />

            <label className="block text-sm font-medium text-slate-600 mt-4 mb-1.5">Senha temporaria</label>
            <input
              type="password"
              value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })}
              required
              minLength={12}
              maxLength={128}
              className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none"
            />

            <label className="block text-sm font-medium text-slate-600 mt-4 mb-1.5">Perfil</label>
            <select
              value={form.role}
              onChange={(e) => setForm({ ...form, role: e.target.value as UserRole })}
              className="w-full border border-slate-300 rounded-xl px-4 py-2.5 text-sm focus:ring-2 focus:ring-blue-500/20 focus:border-blue-500 outline-none bg-white"
            >
              <option value="USER">Usuario</option>
              <option value="ADMIN">Administrador</option>
            </select>

            {error && (
              <p className="mt-4 text-sm text-rose-600 bg-rose-50 border border-rose-100 rounded-xl px-3 py-2">
                {error}
              </p>
            )}

            <button
              type="submit"
              disabled={submitting}
              className="mt-6 w-full px-4 py-2.5 rounded-xl bg-[#1a365d] text-white font-semibold text-sm hover:bg-[#244d7c] disabled:opacity-60 transition-colors"
            >
              {submitting ? "Criando..." : "Criar usuario"}
            </button>
          </form>

          <section className="bg-white border border-slate-200 rounded-2xl shadow-sm overflow-hidden">
            <div className="px-6 py-4 border-b border-slate-100">
              <h2 className="text-base font-semibold text-slate-700">Acessos cadastrados</h2>
            </div>
            {loading ? (
              <div className="p-8 text-sm text-slate-400">Carregando usuarios...</div>
            ) : (
              <div className="divide-y divide-slate-100">
                {usuarios.map((usuario) => (
                  <div key={usuario.id} className="px-6 py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="font-semibold text-sm text-slate-700">{usuario.name}</p>
                        <span className={`text-xs px-2 py-0.5 rounded-full font-medium ${
                          usuario.active ? "bg-emerald-50 text-emerald-700" : "bg-slate-100 text-slate-500"
                        }`}>
                          {usuario.active ? "Ativo" : "Inativo"}
                        </span>
                      </div>
                      <p className="text-xs text-slate-400 mt-1">{usuario.email}</p>
                    </div>
                    <div className="flex items-center gap-3">
                      <span className="text-xs font-medium text-blue-700 bg-blue-50 px-2.5 py-1 rounded-lg">
                        {roleLabels[usuario.role]}
                      </span>
                      {usuario.active && (
                        <button
                          onClick={() => handleDeactivate(usuario.id)}
                          className="text-xs px-3 py-1.5 bg-slate-100 text-slate-600 rounded-lg hover:bg-slate-200 font-medium transition-colors"
                        >
                          Desativar
                        </button>
                      )}
                    </div>
                  </div>
                ))}
                {usuarios.length === 0 && (
                  <div className="p-8 text-sm text-slate-400">Nenhum usuario cadastrado.</div>
                )}
              </div>
            )}
          </section>
        </div>
      </main>
    </>
  );
}
