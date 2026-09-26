// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';
import mermaid from 'astro-mermaid';
// Generates /llms.txt, /llms-full.txt and /llms-small.txt for AI agents.
import starlightLlmsTxt from 'starlight-llms-txt';
// Fails the build on broken internal links, so rotten links never get published.
import starlightLinksValidator from 'starlight-links-validator';

// https://astro.build/config
export default defineConfig({
	// Published as a GitHub Pages project site: https://<owner>.github.io/<repo>/
	// `site` is required by starlight-llms-txt to build absolute URLs.
	// `base` applies to the sitemap, llms*.txt and every link Starlight builds from a slug.
	// For a user site (<owner>.github.io repository) or a custom domain, remove `base`.
	site: 'https://<owner>.github.io',
	base: '/<repo>',
	integrations: [
		// Must come before starlight: the ```mermaid fences have to be replaced before
		// Starlight processes the Markdown.
		mermaid({ theme: 'neutral', autoTheme: true }),
		starlight({
			title: 'example-lib',
			description: "<description>",
			// Refers to a key of `locales`. English is served from the root.
			defaultLocale: 'root',
			// English lives under /<repo>/, Japanese under /<repo>/ja/.
			// A page without a translation falls back to English with a notice.
			locales: {
				root: { label: 'English', lang: 'en' },
				ja: { label: '日本語', lang: 'ja' },
			},
			plugins: [
				starlightLlmsTxt(),
				starlightLinksValidator({
					// The Dokka output is copied into public/ by `./gradlew generateApiDocs`, not
					// built by Starlight, so the validator cannot see it (and it does not exist at
					// all in a plain `pnpm build`). Everything else is still validated.
					exclude: ['/<repo>/api-docs/', '/<repo>/api-docs/**'],
				}),
			],
			social: [{ icon: 'github', label: 'GitHub', href: 'https://github.com/<owner>/<repo>' }],
			editLink: {
				baseUrl: 'https://github.com/<owner>/<repo>/edit/main/docs/',
			},
			// Written by hand (not `autogenerate`) so that people decide the order.
			// The top page (`index`) is not listed: the site title already links to it.
			sidebar: [
				{
					label: 'Get started',
					translations: { ja: 'はじめる' },
					items: [
						{ label: 'Introduction', translations: { ja: 'はじめに' }, slug: 'get-started/introduction' },
						{ label: 'Installation', translations: { ja: 'インストール' }, slug: 'get-started/installation' },
					],
				},
				{
					label: 'Guides',
					translations: { ja: 'ガイド' },
					items: [
						{ label: 'Basic usage', translations: { ja: '基本的な使い方' }, slug: 'guides/basic-usage' },
					],
				},
				{
					label: 'API reference',
					translations: { ja: 'API リファレンス' },
					// Dokka HTML written to docs/public/api-docs/ by `./gradlew generateApiDocs`.
					// It is not a content collection page, so it is a `link`, not a `slug`.
					//
					// Do NOT prefix `base` yourself: Starlight adds it to `link` too, so
					// '/<repo>/api-docs/' would become '/<repo>/<repo>/api-docs/'. On Japanese
					// pages the locale is inserted as well (/<repo>/ja/api-docs/), which
					// public/ja/api-docs/index.html redirects to the single English copy.
					link: '/api-docs/',
				},
			],
		}),
	],
});
