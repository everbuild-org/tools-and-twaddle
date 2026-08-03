// @ts-check
import { defineConfig } from 'astro/config';
import starlight from '@astrojs/starlight';

// https://astro.build/config
export default defineConfig({
	site: 'https://twaddle.asorda.net',
	integrations: [
		starlight({
			title: 'Tools & twaddle',
			description: 'An opinionated toolkit for building modular Minestom servers.',
			favicon: '/favicon.png',
			logo: {
				src: './src/assets/icon.png',
				alt: 'Tools & twaddle toolbox',
			},
			customCss: ['./src/styles/custom.css'],
			social: [
				{
					icon: 'github',
					label: 'GitHub',
					href: 'https://github.com/everbuild-org/tools-and-twaddle',
				},
			],
			sidebar: [
				{
					label: 'Start here',
					items: [
						{ label: 'Overview', slug: 'index' },
						{ label: 'Getting started', slug: 'getting-started' },
					],
				},
				{
					label: 'Concepts',
					items: [
						{ label: 'Architecture', slug: 'concepts/architecture' },
						{ label: 'Capability classification', slug: 'concepts/classification' },
					],
				},
				{
					label: 'Foundation',
					items: [
						{ label: 'Core runtime', slug: 'core/runtime' },
						{ label: 'Structured logging', slug: 'core/logging' },
						{ label: 'Minestom helpers', slug: 'core/minestom-helpers' },
					],
				},
				{
					label: 'Modules',
					items: [
						{ label: 'Inventory transfer rules', slug: 'modules/inventory-transfer' },
					],
				},
			],
		}),
	],
});
