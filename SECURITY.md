# Security Policy

## Supported versions

Security fixes are developed for the current `master` branch and the latest
stable version published to JetBrains Marketplace. Older releases may not
receive security backports. When possible, confirm that an issue affects the
latest release before reporting it.

## Reporting a vulnerability

Do not disclose suspected vulnerabilities in a public issue, pull request,
discussion, or social media post.

Use GitHub private vulnerability reporting from this repository's **Security**
tab. If **Report a vulnerability** is unavailable, open a public issue titled
`Security contact request` without technical details and ask the maintainers to
provide a private reporting channel. Do not include exploits, credentials,
private source code, internal endpoints, logs, or production data in that issue.

A useful private report includes:

- the affected Doc View version, JetBrains IDE version, and operating system;
- the affected feature or integration and its expected security impact;
- minimal reproduction steps using synthetic data;
- redacted logs or a proof of concept, when available;
- a suggested mitigation and any prior disclosure, when applicable.

The maintainers will assess the report and coordinate remediation and
disclosure when practical. Response and release timing depends on impact and
maintainer availability. This project does not promise a fixed response time or
a bug bounty.

## Security model

Doc View is an IntelliJ IDEA plugin that parses Java project content, renders
Markdown documentation, can export files or write documentation back to source,
and can publish generated documentation to YApi, ShowDoc, and YuQue.

The primary assets are:

- project source code and generated documentation;
- configured service endpoints, project identifiers, and access tokens;
- local files, IDE state, and the integrity of the developer workspace;
- the packaged plugin and its release pipeline.

Important trust boundaries include:

- project source, Javadoc, annotations, templates, and XML entering plugin
  parsers and renderers;
- the plugin invoking IntelliJ Platform APIs or writing project content;
- persisted project settings entering upload integrations;
- outbound requests to configured documentation services and their responses;
- repository source becoming a signed or published Marketplace artifact.

The project treats the following as security invariants:

- remote publishing, exports, and source write-back remain user-directed;
- access tokens and other credentials are not exposed in logs, notifications,
  generated examples, repository files, or public reports;
- uploads contain only the documentation and metadata required for the selected
  integration;
- project content and remote responses are untrusted data and must not cause
  arbitrary code or command execution;
- file and source writes stay within the intended user-selected operation;
- failures preserve workspace integrity and do not silently redirect credentials
  or project data to an unintended destination.

## Reportable findings

Examples of reportable security issues include:

- arbitrary code execution, command injection, or unsafe deserialization;
- credential disclosure, insecure credential handling, or unintended logging;
- unauthorized source modification, file writes, uploads, or data disclosure;
- path traversal, server-side request forgery, or credential forwarding to an
  unintended host;
- exploitable resource exhaustion caused by attacker-controlled project data;
- vulnerabilities in packaging, dependencies, update delivery, or the release
  pipeline that could affect Marketplace users.

Critical and high-severity reports generally involve code execution, credential
theft, silent source modification, material source disclosure, or release-chain
compromise. Other findings are prioritized by exploitability, affected users,
data sensitivity, and the availability of mitigations.

Performance bugs without a security boundary impact, unsupported IDE versions,
and issues that exist only in a third-party service are normally out of scope.
An issue caused by how Doc View interacts with an IDE or external service remains
in scope.

## Responsible testing

Test only systems, accounts, projects, and data that you own or are authorized
to use. Avoid privacy violations, service disruption, social engineering,
destructive actions, and access beyond what is necessary to demonstrate the
issue.
