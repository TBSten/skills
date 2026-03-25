import { Hono } from "hono";

const REDIRECT_URL =
	"https://raw.githubusercontent.com/tbsten/skills/main/rules/install.sh";

const app = new Hono();

app.get("/i", (c) => c.redirect(REDIRECT_URL, 302));

export default app;
