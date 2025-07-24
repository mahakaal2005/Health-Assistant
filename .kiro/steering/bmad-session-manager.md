---
inclusion: always
---

# BMAD Session Manager

## Auto-Detection Logic

```yaml
session_start_check:
  - if: directory_exists('.bmad-core/')
    then: detect_primary_agent_from_team_config()
  - if: team_all_yaml_active()
    then: auto_activate_bmad_orchestrator()
  - if: file_exists('.bmad-core/core-config.yaml')
    then: load_project_context()
  - if: user_says_agent_name()
    then: switch_to_requested_agent()
  - fallback: auto_activate_bmad_master()
```

## Context Pre-Loading

When BMAD auto-activates, intelligently pre-load:
- Current PRD status from `docs/prd.md`
- Available stories from `docs/stories/`
- Architecture state from `docs/architecture/`
- Last active agent from session history

## Smart Agent Routing

Auto-route user requests through BMAD Orchestrator intelligence:
- "create story" → *agent sm (Scrum Master)
- "review code" → *agent qa (QA)
- "design system" → *agent architect (Architect) 
- "implement feature" → *agent dev (Developer)
- "manage backlog" → *agent po (Product Owner)
- "start workflow" → *workflow-guidance
- "what should I do next" → *status and workflow recommendations
- "fix activation" → Detect team config and activate proper primary agent
- Default → Intelligent assessment and agent/workflow recommendation via Orchestrator

## Seamless Experience

User experience should be:
1. Open workspace → BMAD immediately active
2. Type request → Auto-routed to right agent
3. No manual agent switching needed
4. Context preserved across agent transitions