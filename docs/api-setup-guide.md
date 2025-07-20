# 🔑 Free Health APIs Setup Guide

## Overview

Your Discover feature now supports **real health content** from multiple free APIs! This guide will help you set up the API keys to get live health articles, news, and educational videos.

## 🆓 **Free APIs Used (All with Generous Limits)**

### 1. **News API** - Health Articles & Breaking News
- **Cost**: FREE (1,000 requests/day)
- **Content**: Latest health news from trusted sources
- **Setup**: 2 minutes

### 2. **Guardian API** - Quality Health Articles  
- **Cost**: FREE (Unlimited requests)
- **Content**: High-quality health journalism
- **Setup**: 2 minutes

### 3. **YouTube Data API v3** - Educational Videos
- **Cost**: FREE (10,000 units/day = ~1,000 video searches)
- **Content**: Educational health videos from verified channels
- **Setup**: 3 minutes

### 4. **Google AI (Gemini)** - Content Enhancement
- **Cost**: You already have Google AI Pro! 🎉
- **Features**: Content validation, summarization, categorization
- **Setup**: 1 minute

---

## 🚀 **Quick Setup (15 minutes total)**

### **Step 1: News API (2 minutes)**
1. Go to [newsapi.org/register](https://newsapi.org/register)
2. Sign up with your email (free account)
3. Verify your email
4. Copy your API key from the dashboard
5. Paste it in `ApiKeyManager.kt` → `newsApiKey`

### **Step 2: Guardian API (2 minutes)**
1. Go to [open-platform.theguardian.com/access](https://open-platform.theguardian.com/access/)
2. Click "Register for an API key"
3. Fill out the form (select "Personal use")
4. Copy your API key from the email
5. **Choose one of two setup methods:**

**Option A: JSON File Method (Recommended)**
- Create file: `app/src/main/assets/api_keys/guardian_api_key.json`
- Add your key:
```json
{
  "api-key": "YOUR_GUARDIAN_API_KEY",
  "format": "json",
  "show-fields": "headline,standfirst,trailText,byline,main,body,thumbnail"
}
```

**Option B: Direct Code Method**
- Paste it in `ApiKeyManager.kt` → `guardianDirectApiKey`

### **Step 3: YouTube Data API v3 (3 minutes)**
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select existing one
3. Enable "YouTube Data API v3" in the API Library
4. Go to "Credentials" → "Create Credentials" → "API Key"
5. Copy your API key
6. Paste it in `ApiKeyManager.kt` → `youtubeApiKey`

### **Step 4: Google AI API (1 minute)**
1. Go to [makersuite.google.com/app/apikey](https://makersuite.google.com/app/apikey)
2. Create API key (you have Google AI Pro access)
3. Copy your API key
4. Paste it in `ApiKeyManager.kt` → `googleAiApiKey`

---

## 📝 **Update API Keys**

Open `app/src/main/java/com/example/health_assistant/features/discover/data/remote/ApiKeyManager.kt`:

```kotlin
@Singleton
class ApiKeyManager @Inject constructor() {
    
    // Replace these with your actual API keys
    val newsApiKey: String = "YOUR_ACTUAL_NEWS_API_KEY"
    val youtubeApiKey: String = "YOUR_ACTUAL_YOUTUBE_API_KEY"
    val googleAiApiKey: String = "YOUR_ACTUAL_GOOGLE_AI_API_KEY"
    
    // Guardian API key - automatically reads from JSON file or falls back to direct key
    val guardianApiKey: String by lazy { readGuardianApiKey() }
    private val guardianDirectApiKey: String = "YOUR_ACTUAL_GUARDIAN_API_KEY" // Fallback
    
    // ... rest of the class
}
```

---

## 🔧 **Guardian API JSON File Benefits**

**Why use the JSON file method?**

✅ **Cleaner Code**: No hardcoded API keys in source code  
✅ **Flexible Configuration**: Easy to change API parameters  
✅ **Better Security**: JSON files can be excluded from version control  
✅ **Advanced Options**: Configure show-fields and other parameters  

**JSON File Structure**:
```json
{
  "api-key": "your-guardian-api-key-here",
  "format": "json",
  "show-fields": "headline,standfirst,trailText,byline,main,body,thumbnail"
}
```

**Available show-fields options**:
- `headline`: Article title
- `standfirst`: Article summary
- `trailText`: Preview text
- `byline`: Author information
- `main`: Main image URL
- `body`: Full article content
- `thumbnail`: Thumbnail image URL

---

## 🎯 **What You'll Get**

### **Real Health Articles**
- Latest medical research from trusted sources
- Nutrition advice from registered dietitians  
- Fitness tips from certified trainers
- Mental health guidance from licensed professionals

### **Breaking Health News**
- CDC updates and health alerts
- Medical breakthrough announcements
- Public health policy changes
- Disease outbreak information

### **Educational Videos**
- Mayo Clinic health education
- TED-Ed medical content
- Crash Course health topics
- Verified expert explanations

### **AI-Enhanced Content**
- **Credibility Scoring**: Each article gets a 1-5 credibility score
- **Smart Categorization**: Auto-categorizes content by health topic
- **Content Summaries**: AI-generated summaries for quick reading
- **Tag Extraction**: Relevant health tags for better search

---

## 🔄 **Fallback System**

**Don't worry if you don't set up APIs immediately!** The app includes:

✅ **Automatic Fallback**: Uses sample data if APIs aren't configured  
✅ **Graceful Degradation**: Real data when available, sample data otherwise  
✅ **Error Handling**: Continues working even if some APIs fail  
✅ **Mixed Sources**: Combines multiple APIs for richer content  

---

## 📊 **API Limits & Usage**

| API | Free Limit | Typical Usage | Overage Cost |
|-----|------------|---------------|--------------|
| News API | 1,000 req/day | ~100 req/day | $449/month for more |
| Guardian | Unlimited | No limits | Always free |
| YouTube | 10,000 units/day | ~1,000 searches/day | $0.05 per 1,000 units |
| Google AI | Your Pro plan | Generous limits | Covered by your plan |

**Estimated daily usage**: ~200 requests total (well within limits)

---

## 🛠️ **Testing Your Setup**

After adding your API keys:

1. **Build the app**: `./gradlew assembleDebug`
2. **Run the app** and navigate to Discover tab
3. **Check logs** for "Successfully fetched X real articles"
4. **Look for real content** with current timestamps

### **Log Messages to Look For**:
```
✅ "Successfully fetched 5 real articles"
✅ "Successfully fetched 3 real news items"  
✅ "Successfully fetched 4 real videos"
```

### **If You See**:
```
⚠️ "API keys not configured, using sample data"
⚠️ "Missing keys: [News API Key, Guardian API Key]"
```
→ Double-check your API keys in `ApiKeyManager.kt`

---

## 🎉 **Benefits of Real Data**

### **For Users**:
- **Fresh Content**: Always up-to-date health information
- **Trusted Sources**: Content from Mayo Clinic, WebMD, Guardian Health
- **Diverse Perspectives**: Multiple sources for comprehensive coverage
- **Video Learning**: Educational content from verified health channels

### **For You**:
- **No Content Management**: APIs provide fresh content automatically
- **Scalable**: Handles growing user base without manual content creation
- **Professional**: Real medical content vs. placeholder text
- **SEO Benefits**: Fresh, relevant content improves app store ranking

---

## 🔒 **Security Notes**

- **API Keys**: Keep your keys secure, don't commit to version control
- **Rate Limiting**: Built-in respect for API rate limits
- **Error Handling**: Graceful fallbacks prevent app crashes
- **Content Validation**: Google AI validates content credibility

---

## 🆘 **Need Help?**

### **Common Issues**:

**"API key invalid"**
→ Double-check you copied the full key without extra spaces

**"Quota exceeded"**  
→ You've hit daily limits, will reset at midnight UTC

**"No content loading"**
→ Check internet connection and API key configuration

### **Support Resources**:
- [News API Documentation](https://newsapi.org/docs)
- [Guardian API Documentation](https://open-platform.theguardian.com/documentation/)
- [YouTube API Documentation](https://developers.google.com/youtube/v3)
- [Google AI Documentation](https://ai.google.dev/docs)

---

## 🚀 **Ready to Go!**

Once you've added your API keys, your Discover feature will have:

🔥 **Live health content** from trusted medical sources  
🤖 **AI-enhanced** summaries and credibility scoring  
📱 **Professional-grade** health information app  
⚡ **Real-time updates** on health news and research  

**Your users will get the same quality content as major health apps - all powered by free APIs!** 🎯