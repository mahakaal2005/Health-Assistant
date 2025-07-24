# BMAD Session Timeout Solution Guide

## 🚨 Problem Analysis

You've been experiencing the **"spec session timeout"** issue where BMAD agents:
1. Start elicitation sessions for document creation
2. Require extensive user interaction (1-9 option selections)
3. Timeout during long sessions and lose context
4. Force you to restart the entire process

## 🔍 Root Cause

### BMAD's Mandatory Elicitation System:
- **create-doc.md**: Requires `elicit: true` sections with 1-9 user interactions
- **create-next-story.md**: Complex multi-step process with multiple elicitation points
- **Session Management**: Long interactive sessions exceed context limits

### Your Specific Issue:
- **PRD is complete** (you have sharded PRD files)
- **Stories are defined** (10 detailed stories in PRD)
- **No need for elicitation** - you just need implementation files
- **BMAD is over-engineering** simple file creation

## ✅ Complete Solution

### 1. **Use Streamlined Workflow (RECOMMENDED)**

Instead of standard agents, use the new streamlined approach:

```bash
# Quick story creation (no timeouts)
*agent streamlined-sm
*quick-draft

# This creates complete story files instantly using your existing PRD
```

### 2. **Direct File Creation Approach**

For immediate results, create story files directly:

```bash
# Create the stories directory
mkdir docs/stories

# Use the orchestrator to create files efficiently
*status  # See current project state
*agent dev  # Switch to developer for file creation
```

### 3. **Bypass Elicitation for Existing Content**

Since your PRD is complete with 10 detailed stories, you can:

**Option A: Manual Story File Creation**
- Copy story content from `docs/prd/requirements.md`
- Create individual `.md` files in `docs/stories/`
- Use the story template structure I provided

**Option B: Batch Story Creation**
```bash
*agent streamlined-sm
*quick-draft  # Creates Story 1.1
*quick-draft  # Creates Story 1.2
# Repeat for all 10 stories
```

## 🎯 Immediate Action Plan

### Step 1: Create Your First Story (2 minutes)
```bash
*agent streamlined-sm
*quick-draft
```

This will create `docs/stories/1.1.design-system-foundation.md` without any elicitation.

### Step 2: Verify Story Creation
Check that the file was created with:
- Complete story structure
- Acceptance criteria from your PRD
- Ready-to-implement tasks
- No missing information

### Step 3: Start Development
```bash
*agent dev
# Point to the created story file
# Begin implementation immediately
```

## 🔧 Technical Fixes Applied

### 1. **Streamlined Configuration**
- Created `.bmad-core/streamlined-config.yaml`
- Optimized for efficiency over elicitation
- Reduces session timeout risks

### 2. **Quick Story Creation Task**
- Created `.bmad-core/tasks/quick-story-create.md`
- Uses existing PRD content
- No elicitation required
- Creates complete stories instantly

### 3. **Streamlined SM Agent**
- Created `.bmad-core/agents/streamlined-sm.md`
- Focused on efficiency
- Avoids timeout-prone workflows

### 4. **Updated Routing**
- Modified steering files to recommend streamlined approach
- Provides clear guidance for new users

## 🚀 Benefits of This Solution

### ✅ **No More Timeouts**
- Streamlined workflow completes in single session
- No extended elicitation periods
- Context preserved throughout

### ✅ **Uses Your Existing Work**
- Leverages your complete PRD
- No redundant information gathering
- Builds on your 10 detailed stories

### ✅ **Ready for Development**
- Creates implementation-ready story files
- Includes acceptance criteria and tasks
- Developer can start immediately

### ✅ **Maintains BMAD Benefits**
- Structured story format
- Proper task breakdown
- Integration with dev workflow

## 🎓 Usage Examples

### Creating All 10 Stories Quickly:
```bash
*agent streamlined-sm

# Create each story (30 seconds each)
*quick-draft  # Story 1.1: Design System Foundation
*quick-draft  # Story 1.2: Remove AI Features  
*quick-draft  # Story 1.3: Navigation UI
*quick-draft  # Story 1.4: Card System
*quick-draft  # Story 1.5: Journal Fragment
*quick-draft  # Story 1.6: Prescription UI
*quick-draft  # Story 1.7: Health Monitoring
*quick-draft  # Story 1.8: Authentication UI
*quick-draft  # Story 1.9: Content Discovery
*quick-draft  # Story 1.10: Integration Testing
```

### Starting Development:
```bash
*agent dev
# Implement Story 1.1 using the created file
# Follow the tasks and acceptance criteria
# No additional elicitation needed
```

## 🔄 Fallback Options

If you still experience issues:

### Option 1: Manual File Creation
I can provide you with complete story file templates that you can copy directly.

### Option 2: Simplified Agent Commands
Use basic file creation commands instead of complex workflows.

### Option 3: Direct Implementation
Skip story files entirely and work directly from your PRD.

## 📞 Next Steps

**Try this now:**
```bash
*agent streamlined-sm
*quick-draft
```

This should create your first story file without any timeouts or elicitation sessions. Let me know if it works or if you encounter any issues!

---

**Status**: ✅ Timeout issue analyzed and streamlined solution implemented
**Ready**: Quick story creation without session management problems