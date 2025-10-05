Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ai = 'C:\Users\Diganta1\AndroidStudioProjects\AINoteBuddy\app\src\main\java\com\ainotebuddy\app\search\AIEnhancedSearchEngine.kt'
if (-not (Test-Path -LiteralPath $ai)) { Write-Error "AIEnhancedSearchEngine.kt not found at $ai"; exit 1 }

$content = Get-Content -Raw -LiteralPath $ai

# 1) Disambiguate SmartSearchEngine type
$content = [regex]::Replace($content, 'private\s+val\s+baseSearchEngine:\s*SmartSearchEngine', 'private val baseSearchEngine: com.ainotebuddy.app.search.SmartSearchEngine')

# 2) NoteEmbedding id must be String
$content = $content -replace 'NoteEmbedding\(note\.id, emptyList\(\)\)', 'NoteEmbedding(note.id.toString(), emptyList())'

# 3) lastModified -> dateModified
$content = $content -replace 'note\.lastModified', 'note.dateModified'

# 4) NoteActivity id String vs Long
$content = [regex]::Replace($content, 'activity\s+is\s+NoteActivity\s+&&\s+activity\.noteId\s*==\s*note\.id', 'activity is NoteActivity && activity.noteId == note.id.toString()')

# 5) Remove category preference relevance block
$pattern1 = '(?ms)^\s*//\s*Category preference relevance\s*\r?\n\s*if\s*\(userContext\.preferences\.preferredCategories\.contains\(note\.category\)\)\s*\{\s*\r?\n\s*relevance\s*\+=\s*0\.2f\s*\r?\n\s*\}\s*\r?\n'
$content = [regex]::Replace($content, $pattern1, '')

# 6) Remove factors.add("Preferred category") block
$pattern2 = '(?ms)^\s*if\s*\(userContext\.preferences\.preferredCategories\.contains\(note\.category\)\)\s*\{\s*\r?\n\s*factors\.add\("Preferred category"\)\s*\r?\n\s*\}\s*\r?\n'
$content = [regex]::Replace($content, $pattern2, '')

# 7) Remove personalization boost in final score
$pattern3 = '(?ms)^\s*//\s*Personalization boost\s*\r?\n\s*if\s*\(userContext\.preferences\.preferredCategories\.contains\(result\.note\.category\)\)\s*\{\s*\r?\n\s*score\s*\+=\s*0\.1f\s*\r?\n\s*\}\s*\r?\n'
$content = [regex]::Replace($content, $pattern3, '')

# 8) Replace sentiment suggestions to use Sentiment enum
$patternSent = '(?ms)^\s*//\s*Sentiment-based suggestions\s*\r?\n\s*if\s*\(queryAnalysis\.sentiment\.polarity\s*!=\s*0f\)\s*\{[\s\S]*?\}\s*\r?\n'
$replacementSent = @'
        // Sentiment-based suggestions
        val sentimentLabel = when (queryAnalysis.sentiment.sentiment) {
            Sentiment.POSITIVE -> "positive"
            Sentiment.NEGATIVE -> "negative"
            else -> null
        }
        if (sentimentLabel != null) {
            suggestions.add(
                AISearchSuggestion(
                    suggestion = "sentiment:$sentimentLabel",
                    type = AISearchSuggestionType.SENTIMENT_FILTER,
                    confidence = queryAnalysis.sentiment.confidence,
                    explanation = "Find $sentimentLabel notes",
                    expectedResults = estimateResultCount("sentiment:$sentimentLabel", notes)
                )
            )
        }
'@
$content = [regex]::Replace($content, $patternSent, $replacementSent)

# 9) Switch SearchResult? to BaseNoteResult?
$content = [regex]::Replace($content, 'baseResult:\s*SearchResult\?', 'baseResult: BaseNoteResult?')
$content = [regex]::Replace($content, 'base:\s*SearchResult\?', 'base: BaseNoteResult?')

# 10) Remove extra brace after cache let block
$patternBrace = '(?ms)(return@withContext\s+cachedResult\s*\r?\n\s*\})\s*\r?\n\s*\}'
$replacementBrace = '$1'
$content = [regex]::Replace($content, $patternBrace, $replacementBrace)

Set-Content -LiteralPath $ai -Value $content -Encoding UTF8
Write-Host 'Patched AIEnhancedSearchEngine.kt successfully.'
