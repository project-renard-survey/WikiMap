package wikimap.suggestion

/**
 * Created by Basim on 07/08/2017.
 */
class SuggestionsCache(val provider: SuggestionProvider) : SuggestionProvider {

    private val cache = mutableMapOf<String, List<String>>()

    override fun getSuggestions(key: String): List<String> {
        if (key == "...") return emptyList()
        return cache.getOrPut(key, { provider.getSuggestions(key) })
    }

}
