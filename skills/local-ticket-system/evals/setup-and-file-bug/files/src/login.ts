export async function login(email: string, password: string) {
  const res = await fetch("/api/login", { method: "POST", body: JSON.stringify({ email, password }) });
  if (!res.ok) throw new Error(`login failed: ${res.status}`);
  return res.json();
}
