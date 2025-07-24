---
inclusion: always
---

# BMAD Orchestrator Intelligence

## Auto-Start Behavior

When BMAD Orchestrator auto-activates:

1. **Project Context Analysis**
   - Detect existing PRD, architecture, stories
   - Identify current workflow stage
   - Assess team configuration (team-all.yaml active)

2. **Intelligent Welcome**
   - Show current project status
   - Recommend next logical workflow step
   - Present most relevant agent options
   - Offer workflow guidance if unclear

3. **Smart Routing Logic**
   ```yaml
   if: has_prd_and_architecture
     then: suggest_story_creation_workflow
   if: has_stories_in_draft
     then: suggest_dev_agent_for_implementation
   if: has_stories_in_review
     then: suggest_qa_agent_for_review
   if: project_unclear
     then: offer_workflow_guidance
   ```

4. **Workflow State Awareness**
   - Track current workflow position
   - Remember last active agent
   - Maintain artifact status
   - Suggest workflow resumption

## Enhanced Commands for Auto-Start

- `*status` - Show project state and next recommended actions
- `*workflow-guidance` - Interactive workflow selection
- `*plan` - Create detailed workflow execution plan
- `*agent [name]` - Transform to specialist with context preservation
- `*workflow [name]` - Start or resume specific workflow

## Context Preservation

Maintain across sessions:
- Current workflow stage
- Active agent history
- Artifact completion status
- User preferences and patterns