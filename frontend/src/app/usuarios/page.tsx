"use client";

import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";
import Navbar from "@/components/Navbar";
import { api } from "@/lib/api";
import type { UserResponse, UserRole } from "@/lib/types";

const roleLabels: Record<UserRole, string> = {
  ADMIN: "Administrador",
  USER: "Usuário",
};

const roleDescriptions: Record<UserRole, string> = {
  ADMIN: "Gerencia usuários e acessa todas as áreas",
  USER: "Acessa transcrições, análises e alertas",
};

type StatusFilter = "todos" | "ativos" | "inativos";

type PendingStatusAction = {
  id: string;
  name: string;
  nextActive: boolean;
} | null;

function UserIcon({ path, className = "h-5 w-5" }: { path: string; className?: string }) {
  return (
    <svg aria-hidden="true" className={className} fill="none" viewBox="0 0 24 24" stroke="currentColor" strokeWidth={1.6}>
      <path strokeLinecap="round" strokeLinejoin="round" d={path} />
    </svg>
  );
}

function SummaryCard({
  title,
  value,
  description,
  tone,
}: {
  title: string;
  value: number;
  description: string;
  tone: "blue" | "emerald" | "slate" | "violet";
}) {
  const styles = {
    blue: "border-blue-200 bg-blue-50 text-blue-800",
    emerald: "border-emerald-200 bg-emerald-50 text-emerald-800",
    slate: "border-slate-200 bg-white text-slate-900",
    violet: "border-violet-200 bg-violet-50 text-violet-800",
  }[tone];

  return (
    <div className={`rounded-2xl border p-5 shadow-sm shadow-slate-200/60 ${styles}`}>
      <p className="text-sm font-semibold opacity-80">{title}</p>
      <p className="mt-2 text-3xl font-bold tracking-tight">{value}</p>
      <p className="mt-1 text-sm opacity-75">{description}</p>
    </div>
  );
}

export default function UsuariosPage() {
  const [usuarios, setUsuarios] = useState<UserResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [pendingAction, setPendingAction] = useState<PendingStatusAction>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState<StatusFilter>("todos");
  const [roleFilter, setRoleFilter] = useState<UserRole | "">("");
  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    role: "USER" as UserRole,
  });

  const currentUser = api.auth.user();

  const loadUsers = useCallback(() => {
    setLoading(true);
    setError("");
    api.users
      .listar()
      .then(setUsuarios)
      .catch((e) => setError(e instanceof Error ? e.message : "Erro ao carregar usuários"))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  const stats = useMemo(() => {
    const active = usuarios.filter((user) => user.active).length;
    const admins = usuarios.filter((user) => user.role === "ADMIN").length;
    return {
      total: usuarios.length,
      active,
      inactive: usuarios.length - active,
      admins,
    };
  }, [usuarios]);

  const filteredUsers = useMemo(() => {
    const query = search.trim().toLowerCase();
    return usuarios.filter((user) => {
      if (query && !`${user.name} ${user.email}`.toLowerCase().includes(query)) return false;
      if (statusFilter === "ativos" && !user.active) return false;
      if (statusFilter === "inativos" && user.active) return false;
      if (roleFilter && user.role !== roleFilter) return false;
      return true;
    });
  }, [usuarios, search, statusFilter, roleFilter]);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    setSubmitting(true);
    setError("");
    setSuccess("");
    try {
      const created = await api.users.criar(form);
      setUsuarios((current) => [created, ...current]);
      setForm({ name: "", email: "", password: "", role: "USER" });
      setSuccess("Usuário criado com sucesso.");
    } catch (e) {
      setError(e instanceof Error ? e.message : "Erro ao criar usuário");
    } finally {
      setSubmitting(false);
    }
  };

  const confirmStatusChange = async () => {
    if (!pendingAction) return;
    setError("");
    setSuccess("");
    try {
      const updated = pendingAction.nextActive
        ? await api.users.ativar(pendingAction.id)
        : await api.users.desativar(pendingAction.id);
      setUsuarios((current) => current.map((user) => (user.id === updated.id ? updated : user)));
      setSuccess(
        pendingAction.nextActive
          ? `${updated.name} foi ativado novamente.`
          : `${updated.name} foi desativado.`
      );
      setPendingAction(null);
    } catch (e) {
      setError(e instanceof Error ? e.message : "Não foi possível atualizar o usuário");
    }
  };

  const clearFilters = () => {
    setSearch("");
    setStatusFilter("todos");
    setRoleFilter("");
  };

  const hasFilters = Boolean(search || statusFilter !== "todos" || roleFilter);

  return (
    <>
      <Navbar />
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <header className="mb-8 overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-sm shadow-slate-200/70">
          <div className="bg-gradient-to-r from-[#16355f] via-[#1f5d9d] to-[#2a78bf] px-6 py-7 text-white">
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-100">Administração</p>
            <div className="mt-3 flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">Usuários</h1>
                <p className="mt-2 max-w-2xl text-sm leading-6 text-blue-50">
                  Gerencie acessos, perfis e status dos usuários que usam a plataforma TOTVS Conversational Intelligence.
                </p>
              </div>
              <button
                type="button"
                onClick={loadUsers}
                disabled={loading}
                className="inline-flex items-center justify-center gap-2 rounded-xl border border-white/30 bg-white/10 px-4 py-2.5 text-sm font-semibold text-white transition hover:bg-white/20 disabled:cursor-not-allowed disabled:opacity-60"
              >
                <UserIcon path="M16.023 9.348h4.992v-.001M2.985 19.644v-4.992m0 0h4.992m-4.993 0l3.181 3.183a8.25 8.25 0 0013.803-3.7M21.015 4.356v4.992m0 0h-4.992m4.992 0l-3.181-3.183a8.25 8.25 0 00-13.803 3.7" className="h-4 w-4" />
                Atualizar lista
              </button>
            </div>
          </div>
          <div className="grid gap-4 px-6 py-5 sm:grid-cols-2 lg:grid-cols-4">
            <SummaryCard title="Total" value={stats.total} description="Acessos cadastrados" tone="slate" />
            <SummaryCard title="Ativos" value={stats.active} description="Podem acessar o painel" tone="emerald" />
            <SummaryCard title="Inativos" value={stats.inactive} description="Bloqueados para login" tone="blue" />
            <SummaryCard title="Admins" value={stats.admins} description="Permissões elevadas" tone="violet" />
          </div>
        </header>

        {(error || success) && (
          <div
            className={`mb-6 rounded-2xl border px-4 py-3 text-sm font-medium ${
              error
                ? "border-rose-200 bg-rose-50 text-rose-700"
                : "border-emerald-200 bg-emerald-50 text-emerald-700"
            }`}
          >
            {error || success}
          </div>
        )}

        <div className="grid grid-cols-1 gap-6 lg:grid-cols-[400px_1fr]">
          <form onSubmit={handleSubmit} className="h-fit rounded-3xl border border-slate-200 bg-white p-6 shadow-sm shadow-slate-200/70">
            <div className="mb-6 flex items-start gap-4">
              <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-[#1f5d9d] ring-1 ring-blue-100">
                <UserIcon path="M18 7.5v3m0 0v3m0-3h3m-3 0h-3M6.75 6.75a3.75 3.75 0 117.5 0 3.75 3.75 0 01-7.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0A17.933 17.933 0 0112 21.75c-2.676 0-5.216-.584-7.499-1.632z" />
              </div>
              <div>
                <h2 className="text-lg font-bold text-slate-900">Novo usuário</h2>
                <p className="mt-1 text-sm leading-6 text-slate-500">
                  Crie uma conta com senha temporária e perfil de acesso.
                </p>
              </div>
            </div>

            <div className="grid gap-4">
              <div>
                <label htmlFor="user-name" className="mb-1.5 block text-sm font-semibold text-slate-700">
                  Nome completo
                </label>
                <input
                  id="user-name"
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  required
                  maxLength={120}
                  placeholder="Ex.: Ana Souza"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#2b6cb0] focus:bg-white focus:ring-4 focus:ring-blue-500/10"
                />
              </div>

              <div>
                <label htmlFor="user-email" className="mb-1.5 block text-sm font-semibold text-slate-700">
                  E-mail corporativo
                </label>
                <input
                  id="user-email"
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  required
                  placeholder="usuário@empresa.com"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#2b6cb0] focus:bg-white focus:ring-4 focus:ring-blue-500/10"
                />
              </div>

              <div>
                <label htmlFor="user-password" className="mb-1.5 block text-sm font-semibold text-slate-700">
                  Senha temporária
                </label>
                <input
                  id="user-password"
                  type="password"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  required
                  minLength={12}
                  maxLength={128}
                  placeholder="Mínimo de 12 caracteres"
                  className="w-full rounded-xl border border-slate-300 bg-slate-50 px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#2b6cb0] focus:bg-white focus:ring-4 focus:ring-blue-500/10"
                />
                <p className="mt-1.5 text-xs text-slate-500">Use uma senha forte e compartilhe por canal seguro.</p>
              </div>

              <div>
                <label htmlFor="user-role" className="mb-1.5 block text-sm font-semibold text-slate-700">
                  Perfil
                </label>
                <select
                  id="user-role"
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value as UserRole })}
                  className="w-full rounded-xl border border-slate-300 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-[#2b6cb0] focus:ring-4 focus:ring-blue-500/10"
                >
                  <option value="USER">Usuário</option>
                  <option value="ADMIN">Administrador</option>
                </select>
                <p className="mt-1.5 text-xs text-slate-500">{roleDescriptions[form.role]}</p>
              </div>
            </div>

            <button
              type="submit"
              disabled={submitting}
              className="mt-6 inline-flex w-full items-center justify-center gap-2 rounded-xl bg-[#1f5d9d] px-4 py-3 text-sm font-bold text-white shadow-sm transition hover:bg-[#184b80] disabled:cursor-not-allowed disabled:opacity-60"
            >
              {submitting ? (
                <>
                  <span className="h-4 w-4 animate-spin rounded-full border-2 border-white/30 border-t-white" />
                  Criando acesso...
                </>
              ) : (
                <>
                  <UserIcon path="M12 4.5v15m7.5-7.5h-15" className="h-4 w-4" />
                  Criar usuário
                </>
              )}
            </button>
          </form>

          <section className="rounded-3xl border border-slate-200 bg-white shadow-sm shadow-slate-200/70">
            <div className="border-b border-slate-100 p-5">
              <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                <div>
                  <h2 className="text-lg font-bold text-slate-900">Acessos cadastrados</h2>
                  <p className="mt-1 text-sm text-slate-500">
                    {filteredUsers.length} de {usuarios.length} usuários exibidos
                  </p>
                </div>
                <div className="flex flex-col gap-3 sm:flex-row">
                  <div className="relative">
                    <UserIcon path="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z" className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-slate-400" />
                    <input
                      type="search"
                      value={search}
                      onChange={(e) => setSearch(e.target.value)}
                      placeholder="Buscar nome ou e-mail"
                      className="w-full rounded-xl border border-slate-200 bg-slate-50 py-2.5 pl-9 pr-3 text-sm outline-none transition focus:border-blue-500 focus:bg-white focus:ring-4 focus:ring-blue-500/10 sm:w-64"
                    />
                  </div>
                  <select
                    value={statusFilter}
                    onChange={(e) => setStatusFilter(e.target.value as StatusFilter)}
                    className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                  >
                    <option value="todos">Todos status</option>
                    <option value="ativos">Ativos</option>
                    <option value="inativos">Inativos</option>
                  </select>
                  <select
                    value={roleFilter}
                    onChange={(e) => setRoleFilter(e.target.value as UserRole | "")}
                    className="rounded-xl border border-slate-200 bg-white px-3 py-2.5 text-sm font-medium text-slate-700 outline-none focus:border-blue-500 focus:ring-4 focus:ring-blue-500/10"
                  >
                    <option value="">Todos perfis</option>
                    <option value="ADMIN">Administradores</option>
                    <option value="USER">Usuários</option>
                  </select>
                </div>
              </div>
              {hasFilters && (
                <button
                  type="button"
                  onClick={clearFilters}
                  className="mt-4 text-sm font-semibold text-blue-700 hover:text-blue-800 hover:underline"
                >
                  Limpar filtros
                </button>
              )}
            </div>

            {pendingAction && (
              <div className="border-b border-amber-200 bg-amber-50 px-5 py-4">
                <div className="flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
                  <p className="text-sm text-amber-900">
                    Confirmar {pendingAction.nextActive ? "ativação" : "desativação"} de{" "}
                    <span className="font-bold">{pendingAction.name}</span>?
                  </p>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => setPendingAction(null)}
                      className="rounded-lg bg-white px-3 py-2 text-xs font-bold text-slate-700 ring-1 ring-slate-200 hover:bg-slate-50"
                    >
                      Cancelar
                    </button>
                    <button
                      type="button"
                      onClick={confirmStatusChange}
                      className={`rounded-lg px-3 py-2 text-xs font-bold text-white ${
                        pendingAction.nextActive ? "bg-emerald-600 hover:bg-emerald-700" : "bg-rose-600 hover:bg-rose-700"
                      }`}
                    >
                      Confirmar
                    </button>
                  </div>
                </div>
              </div>
            )}

            {loading ? (
              <div className="grid gap-3 p-5">
                {Array.from({ length: 4 }).map((_, index) => (
                  <div key={index} className="h-20 animate-pulse rounded-2xl bg-slate-100" />
                ))}
              </div>
            ) : filteredUsers.length === 0 ? (
              <div className="px-6 py-16 text-center">
                <div className="mx-auto mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-slate-50 text-slate-400 ring-1 ring-slate-200">
                  <UserIcon path="M15.75 6a3.75 3.75 0 11-7.5 0 3.75 3.75 0 017.5 0zM4.501 20.118a7.5 7.5 0 0114.998 0" />
                </div>
                <p className="font-semibold text-slate-700">Nenhum usuário encontrado</p>
                <p className="mt-1 text-sm text-slate-500">Ajuste os filtros ou crie um novo acesso.</p>
              </div>
            ) : (
              <div className="divide-y divide-slate-100">
                {filteredUsers.map((usuario) => {
                  const isCurrentUser = currentUser?.id === usuario.id;
                  return (
                    <article key={usuario.id} className="px-5 py-4 transition hover:bg-slate-50/70">
                      <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                        <div className="flex min-w-0 items-start gap-4">
                          <div
                            className={`flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl text-sm font-bold ${
                              usuario.active
                                ? "bg-blue-50 text-blue-700 ring-1 ring-blue-100"
                                : "bg-slate-100 text-slate-500 ring-1 ring-slate-200"
                            }`}
                          >
                            {(usuario.name || usuario.email || "U").slice(0, 1).toUpperCase()}
                          </div>
                          <div className="min-w-0">
                            <div className="flex flex-wrap items-center gap-2">
                              <h3 className="truncate text-sm font-bold text-slate-900">{usuario.name}</h3>
                              {isCurrentUser && (
                                <span className="rounded-full bg-blue-100 px-2 py-0.5 text-xs font-bold text-blue-700">
                                  Você
                                </span>
                              )}
                              <span
                                className={`rounded-full px-2.5 py-1 text-xs font-bold ${
                                  usuario.active
                                    ? "bg-emerald-50 text-emerald-700 ring-1 ring-emerald-200"
                                    : "bg-slate-100 text-slate-600 ring-1 ring-slate-200"
                                }`}
                              >
                                {usuario.active ? "Ativo" : "Inativo"}
                              </span>
                              <span className="rounded-full bg-blue-50 px-2.5 py-1 text-xs font-bold text-blue-700 ring-1 ring-blue-100">
                                {roleLabels[usuario.role]}
                              </span>
                            </div>
                            <p className="mt-1 truncate text-sm text-slate-500">{usuario.email}</p>
                            <p className="mt-1 text-xs text-slate-400">
                              Criado em {new Date(usuario.createdAt).toLocaleString("pt-BR")}
                            </p>
                          </div>
                        </div>

                        <div className="flex flex-col gap-2 sm:flex-row sm:justify-end">
                          {usuario.active ? (
                            <button
                              type="button"
                              disabled={isCurrentUser}
                              onClick={() => setPendingAction({ id: usuario.id, name: usuario.name, nextActive: false })}
                              className="inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-white px-3 py-2 text-xs font-bold text-rose-700 transition hover:bg-rose-50 disabled:cursor-not-allowed disabled:border-slate-200 disabled:text-slate-400 disabled:hover:bg-white"
                            >
                              <UserIcon path="M18.364 18.364A9 9 0 005.636 5.636m12.728 12.728L5.636 5.636m12.728 12.728L21 21M5.636 5.636L3 3" className="h-4 w-4" />
                              {isCurrentUser ? "Usuário atual" : "Desativar"}
                            </button>
                          ) : (
                            <button
                              type="button"
                              onClick={() => setPendingAction({ id: usuario.id, name: usuario.name, nextActive: true })}
                              className="inline-flex items-center justify-center gap-2 rounded-xl bg-emerald-600 px-3 py-2 text-xs font-bold text-white transition hover:bg-emerald-700"
                            >
                              <UserIcon path="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z" className="h-4 w-4" />
                              Ativar usuário
                            </button>
                          )}
                        </div>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        </div>
      </main>
    </>
  );
}
