import { useState, type FormEvent } from "react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { api, ApiError } from "../api/client";
import type { ApiKey, ApiKeyCreated, Member, Project, Role } from "../api/types";
import { ProjectNav } from "../components/ProjectNav";
import { formatDateTime } from "../lib/format";

export function SettingsPage() {
  const { projectId = "" } = useParams();
  const queryClient = useQueryClient();
  const [keyName, setKeyName] = useState("");
  const [createdKey, setCreatedKey] = useState<ApiKeyCreated | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [memberEmail, setMemberEmail] = useState("");
  const [memberRole, setMemberRole] = useState<Role>("VIEWER");

  const projectQuery = useQuery({
    queryKey: ["project", projectId],
    queryFn: () => api<Project>(`/api/v1/projects/${projectId}`),
    enabled: Boolean(projectId),
  });

  const keysQuery = useQuery({
    queryKey: ["apiKeys", projectId],
    queryFn: () => api<ApiKey[]>(`/api/v1/projects/${projectId}/api-keys`),
    enabled: Boolean(projectId),
  });

  const membersQuery = useQuery({
    queryKey: ["members", projectId],
    queryFn: () => api<Member[]>(`/api/v1/projects/${projectId}/members`),
    enabled: Boolean(projectId),
  });

  const createKeyMutation = useMutation({
    mutationFn: () =>
      api<ApiKeyCreated>(`/api/v1/projects/${projectId}/api-keys`, {
        method: "POST",
        body: JSON.stringify({ name: keyName }),
      }),
    onSuccess: async (data) => {
      setCreatedKey(data);
      setKeyName("");
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ["apiKeys", projectId] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Falha ao criar chave"),
  });

  const deleteKeyMutation = useMutation({
    mutationFn: (keyId: string) =>
      api<void>(`/api/v1/projects/${projectId}/api-keys/${keyId}`, { method: "DELETE" }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["apiKeys", projectId] });
    },
  });

  const addMemberMutation = useMutation({
    mutationFn: () =>
      api<Member>(`/api/v1/projects/${projectId}/members`, {
        method: "POST",
        body: JSON.stringify({ email: memberEmail, role: memberRole }),
      }),
    onSuccess: async () => {
      setMemberEmail("");
      setError(null);
      await queryClient.invalidateQueries({ queryKey: ["members", projectId] });
    },
    onError: (err) => setError(err instanceof ApiError ? err.message : "Falha ao adicionar membro"),
  });

  function handleCreateKey(event: FormEvent) {
    event.preventDefault();
    createKeyMutation.mutate();
  }

  function handleAddMember(event: FormEvent) {
    event.preventDefault();
    addMemberMutation.mutate();
  }

  const project = projectQuery.data;
  const snippet = project
    ? `<script defer src="${window.location.origin}/js/analytics.js" data-key="${project.publicKey}"></script>`
    : "";

  return (
    <div className="stack">
      <div>
        <h1 style={{ fontSize: 20, margin: "0 0 12px" }}>Configurações</h1>
        <ProjectNav projectId={projectId} />
      </div>

      {error && <p className="error">{error}</p>}

      <div className="panel">
        <h2>Snippet de instalação</h2>
        <p className="muted" style={{ fontSize: 13, marginTop: 0 }}>
          Cole antes de <code>&lt;/head&gt;</code>. A chave pública só permite enviar eventos.
        </p>
        <div className="code">{snippet}</div>
        <p className="muted" style={{ fontSize: 13 }}>
          Eventos customizados: <code>window.analytics.track("signup", {"{ plan: \"pro\" }"})</code>
        </p>
      </div>

      <div className="panel">
        <h2>API keys (server-side)</h2>
        <form className="row" onSubmit={handleCreateKey} style={{ marginBottom: 14 }}>
          <input
            placeholder="Nome da chave"
            value={keyName}
            onChange={(e) => setKeyName(e.target.value)}
            required
            style={{ maxWidth: 280 }}
          />
          <button className="btn btn-primary" type="submit" disabled={createKeyMutation.isPending}>
            Gerar
          </button>
        </form>

        {createdKey && (
          <div className="panel" style={{ background: "var(--panel-2)", marginBottom: 14 }}>
            <div className="muted" style={{ fontSize: 13, marginBottom: 8 }}>
              Copie agora — a chave não será exibida novamente.
            </div>
            <div className="code">{createdKey.apiKey}</div>
          </div>
        )}

        <table className="table">
          <thead>
            <tr>
              <th>Nome</th>
              <th>Prefixo</th>
              <th>Criada em</th>
              <th>Último uso</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {(keysQuery.data ?? []).map((key) => (
              <tr key={key.id}>
                <td>{key.name}</td>
                <td>{key.keyPrefix}…</td>
                <td>{formatDateTime(key.createdAt)}</td>
                <td>{key.lastUsedAt ? formatDateTime(key.lastUsedAt) : "-"}</td>
                <td className="right">
                  <button
                    className="btn btn-sm btn-danger"
                    type="button"
                    onClick={() => deleteKeyMutation.mutate(key.id)}
                  >
                    Revogar
                  </button>
                </td>
              </tr>
            ))}
            {(keysQuery.data ?? []).length === 0 && (
              <tr>
                <td colSpan={5} className="muted">
                  Nenhuma chave.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <div className="panel">
        <h2>Membros</h2>
        <form className="row" onSubmit={handleAddMember} style={{ marginBottom: 14 }}>
          <input
            type="email"
            placeholder="email@exemplo.com"
            value={memberEmail}
            onChange={(e) => setMemberEmail(e.target.value)}
            required
            style={{ maxWidth: 280 }}
          />
          <select
            value={memberRole}
            onChange={(e) => setMemberRole(e.target.value as Role)}
            style={{ maxWidth: 140 }}
          >
            <option value="VIEWER">VIEWER</option>
            <option value="ADMIN">ADMIN</option>
            <option value="OWNER">OWNER</option>
          </select>
          <button className="btn btn-primary" type="submit" disabled={addMemberMutation.isPending}>
            Adicionar
          </button>
        </form>

        <table className="table">
          <thead>
            <tr>
              <th>E-mail</th>
              <th>Nome</th>
              <th>Papel</th>
            </tr>
          </thead>
          <tbody>
            {(membersQuery.data ?? []).map((member) => (
              <tr key={member.userId}>
                <td>{member.email}</td>
                <td>{member.name ?? "-"}</td>
                <td>
                  <span className="badge">{member.role}</span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
