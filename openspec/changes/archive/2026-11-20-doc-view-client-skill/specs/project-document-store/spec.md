## ADDED Requirements

### Requirement: freshness excludes IDEA workspace UI metadata
项目文档 freshness SHALL ignore edits to `.idea/workspace.xml` because it stores IDE workspace UI state rather than Java or Dubbo XML inputs. The same path rule SHALL apply to VFS events and PSI `Document` changes.

#### Scenario: Workspace metadata changes after snapshot commit
- **WHEN** the current-session snapshot is current and IDEA observes an edit to `.idea/workspace.xml` through VFS refresh or a PSI `Document` change
- **THEN** the input epoch does not advance and freshness remains `current`
- **AND** no document files are generated or changed by the read-only freshness check

#### Scenario: Real source or project configuration changes
- **WHEN** IDEA observes Java/XML edits other than `.idea/workspace.xml`, a project content-root change, or a tracked settings/template change after snapshot commit
- **THEN** freshness becomes `stale`
- **AND** other XML files under `.idea` and an ordinary XML file named `workspace.xml` outside `.idea` remain tracked inputs through VFS changes
- **AND** ordinary XML `Document` changes remain tracked inputs
