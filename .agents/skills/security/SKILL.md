---
name: security
description: "Use when hardening a project, handling secrets, environment variables, Docker security, dependency risks, auth, validation, or basic cyber hygiene."
---

# Security Skill

Use this skill when the project needs a security-first review or implementation.

## Focus Areas
- Keep secrets out of source code and use environment variables.
- Prefer `.env.example` for shared defaults and keep `.env` local only.
- Validate inputs at API boundaries.
- Avoid hardcoded credentials in Docker, application config, or scripts.
- Review dependency and container security before production.

## Practical Rules
- Never commit real passwords, tokens, or private keys.
- Use separate values for local development and production.
- Keep Docker Compose values configurable through environment variables.
- Prefer least privilege for users, services, and containers.
- Treat production settings as explicit, not implied by defaults.

## Typical Checks
- Search for hardcoded secrets.
- Verify `.gitignore` covers local environment files.
- Confirm Docker and Spring Boot read from environment variables.
- Check that public endpoints do not expose sensitive data.