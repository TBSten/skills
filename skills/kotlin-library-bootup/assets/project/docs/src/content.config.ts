import { defineCollection } from 'astro:content';
import { docsLoader, i18nLoader } from '@astrojs/starlight/loaders';
import { docsSchema, i18nSchema } from '@astrojs/starlight/schema';

export const collections = {
	docs: defineCollection({ loader: docsLoader(), schema: docsSchema() }),
	// Declared because astro.config.mjs sets `locales`; without it every build warns that
	// the "i18n" collection does not exist. Starlight's own UI strings are already
	// translated, so src/content/i18n/*.json stay empty until we override one.
	i18n: defineCollection({ loader: i18nLoader(), schema: i18nSchema() }),
};
