package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.config.OtherSettings;
import searchengine.dto.searching.SearchingData;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.searching.SearchingResponseServ;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.*;

@Service
@RequiredArgsConstructor
public class SearchingServiceImpl implements SearchingService {
    private LemmaFinder lemmaFinder = null;
    private final LuceneMorphology luceneMorphology;
    private final OtherSettings otherSettings;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private LemmaFinder getLemmaFinder() {
        if (lemmaFinder == null) {
            lemmaFinder = new LemmaFinder(luceneMorphology);
        }
        return lemmaFinder;
    }

    private int calcMinCountPagesToExcludeLemmas(Integer percent, int count) {
        if (percent == null) {
            return count;
        }

        return percent * count / 100;
    }

    private Long getSiteId(String site) {
        if (site == null) {
            return null;
        }
        List<SiteEntity> siteEntityList = siteRepository.findByUrl(site);
        for (SiteEntity siteEntity : siteEntityList) {
            return siteEntity.getId();
        }
        return null;
    }

    private int getCountPages(Long siteId) {
        if (siteId == null) {
            return (int) pageRepository.count();
        }

        return pageRepository.countBySiteId(siteId);
    }

    private String getSnippet(Document document, Set<String> lemmasFromUser, int maxWordsBefore, int maxWordsAfter) {
        String text = document.text();
        String[] words = text.split("\\s+");

        int indexWord = -1;
        for (int i = 0; i < words.length; i++) {
            String word = words[i];
            Set<String> lemmasWord = getLemmaFinder().getLemmaSet(word);
            for (String lemma : lemmasWord) {
                if (lemmasFromUser.contains(lemma)) {
                    indexWord = i;
                    break;
                }
            }
            if (indexWord != -1) {
                break;
            }
        }

        int indexWordInText = text.indexOf(words[indexWord]);
        int indexBeginWordInText = indexWordInText;
        if (indexWordInText > 0) {
            int indexBeginWord = indexWord - maxWordsBefore;
            if (indexBeginWord < 0) {
                indexBeginWord = 0;
            }
            indexBeginWordInText = text.lastIndexOf(words[indexBeginWord], indexWordInText - 1);
        }

        StringBuilder result = new StringBuilder();
        result.append(text.substring(indexBeginWordInText, indexWordInText));
        result.append("<b>").append(words[indexWord]).append("</b>");

        int indexEndWord = indexWord + maxWordsAfter;
        if (indexEndWord >= words.length) {
            indexEndWord = words.length - 1;
        }

        indexBeginWordInText = indexWordInText + words[indexWord].length();
        int indexEndWordInText = text.indexOf(words[indexEndWord], indexBeginWordInText) + words[indexEndWord].length();
        for (int i = indexWord + 1; i <= indexEndWord; i++) {
            String word = words[i];
            Set<String> lemmasWord = getLemmaFinder().getLemmaSet(word);
            for (String lemma : lemmasWord) {
                if (lemmasFromUser.contains(lemma)) {
                    int indexInText = text.indexOf(word, indexBeginWordInText);
                    result.append(text.substring(indexBeginWordInText, indexInText));
                    result.append("<b>").append(words[i]).append("</b>");
                    indexBeginWordInText = indexInText + words[i].length();
                }
            }
        }
        if (indexBeginWordInText < indexEndWordInText) {
            result.append(text.substring(indexBeginWordInText, indexEndWordInText));
        }

        return result.toString();
    }

    private Set<LemmaEntity> getLemmaSorted(Set<String> lemmasFromUser, int minCountPagesToExcludeLemmas
            , Long siteId) {
        Set<LemmaEntity> lemmasSorted = new TreeSet<>(new Comparator<LemmaEntity>() {
            @Override
            public int compare(LemmaEntity o1, LemmaEntity o2) {
                return o1.getFrequency() - o2.getFrequency();
            }
        });

        for (String lemma : lemmasFromUser) {
            List<LemmaEntity> lemmaEntityList = lemmaRepository.findByLemma(lemma);
            for (LemmaEntity lemmaEntity : lemmaEntityList) {
                if (lemmaEntity.getFrequency() <= minCountPagesToExcludeLemmas) {
                    if ((siteId == null) || (lemmaEntity.getSiteEntity().getId() == siteId)) {
                        lemmasSorted.add(lemmaEntity);
                    }
                }
            }
        }

        return lemmasSorted;
    }

    private List<PageEntity> filterPage(Set<LemmaEntity> lemmasSorted) {
        Iterator<LemmaEntity> iteratorLemmasSorted = lemmasSorted.iterator();
        LemmaEntity lemmaEntity = iteratorLemmasSorted.next();
        List<PageEntity> pageEntityList = pageRepository.findByLemma(lemmaEntity.getLemma());

        while (iteratorLemmasSorted.hasNext()) {
            lemmaEntity = iteratorLemmasSorted.next();
            for (int i = 0; i < pageEntityList.size(); i++) {
                PageEntity pageEntity = pageEntityList.get(i);
                if (pageEntity != null) {
                    if (pageRepository.countByIdAndLemma(pageEntity.getId(), lemmaEntity.getLemma()) == 0) {
                        pageEntityList.set(i, null);
                    }
                }
            }
        }

        return pageEntityList;
    }

    private List<Relevance> calcAndSortRelevance(List<PageEntity> pageEntityList, Set<LemmaEntity> lemmasSorted) {
        List<Relevance> relevanceList = new ArrayList<>();
        float maxAbsoluteRelevance = 0;
        for (PageEntity pageEntity : pageEntityList) {
            if (pageEntity != null) {
                Relevance relevance = new Relevance(pageEntity);
                relevanceList.add(relevance);
                for (LemmaEntity lemmaEntity : lemmasSorted) {
                    float rank = indexRepository.getRankByPageAndLemma(pageEntity.getId(), lemmaEntity.getLemma());
                    relevance.setAbsoluteRelevance(relevance.getAbsoluteRelevance() + rank);
                }
                if (maxAbsoluteRelevance < relevance.getAbsoluteRelevance()) {
                    maxAbsoluteRelevance = relevance.getAbsoluteRelevance();
                }
            }
        }
        for (Relevance relevance : relevanceList) {
            relevance.setRelativeRelevance(relevance.getAbsoluteRelevance() / maxAbsoluteRelevance);
        }
        Collections.sort(relevanceList, new Comparator<Relevance>() {
            @Override
            public int compare(Relevance o1, Relevance o2) {
                if (o1.getRelativeRelevance() > o2.getRelativeRelevance()) {
                    return -1;
                } else if (o1.getRelativeRelevance() < o2.getRelativeRelevance()) {
                    return 1;
                }
                return 0;
            }
        });

        return relevanceList;
    }

    private SearchingResponse formingResponse(List<Relevance> relevanceList, Set<String> lemmasFromUser
            , Integer offset, Integer limit) {
        SearchingResponse response = new SearchingResponse();
        response.setResult(true);
        response.setError(null);
        List<SearchingData> data = new ArrayList<>();
        response.setData(data);
        response.setCount(relevanceList.size());
        for (Relevance relevance : relevanceList) {
            if (offset <= 0) {
                if (limit <= 0) {
                    break;
                }
                PageEntity pageEntity = relevance.getPageEntity();

                SearchingData searchingData = new SearchingData();
                List<SiteEntity> siteEntityList = siteRepository.findByPageId(pageEntity.getId());
                SiteEntity siteEntity = siteEntityList.get(0);

                searchingData.setSite(siteEntity.getUrl());
                searchingData.setSiteName(siteEntity.getName());
                searchingData.setUri(pageEntity.getPath());
                Document document = Jsoup.parse(pageEntity.getContent());
                searchingData.setTitle(document.title());

                String snippet =
                        getSnippet(document, lemmasFromUser
                                , otherSettings.getMaxWordsBefore(), otherSettings.getMaxWordsAfter()
                        );

                searchingData.setSnippet(snippet);
                searchingData.setRelevance(relevance.getRelativeRelevance());
                data.add(searchingData);
                limit--;
            } else {
                offset--;
            }
        }

        return response;
    }

    private SearchingResponse formingResponseError(String error) {
        SearchingResponse response = new SearchingResponse();

        response.setResult(false);
        response.setError(error);
        response.setCount(null);
        response.setData(null);

        return response;
    }

    @Override
    public SearchingResponseServ search(String query, String site, Integer offset, Integer limit) {

        SearchingResponseServ responseServ = new SearchingResponseServ();
        try {
            if (query == null || query.trim().isEmpty()) {
                SearchingResponse response = formingResponseError(otherSettings.getErrorMessageEmptySearchQuery());
                responseServ.setSearchingResponse(response);
                responseServ.setStatus(HttpStatus.BAD_REQUEST);
                return responseServ;
            }

            Long siteId = getSiteId(site);
            int countPages = getCountPages(siteId);
            int minCountPagesToExcludeLemmas =
                    calcMinCountPagesToExcludeLemmas(otherSettings.getMinPercentagePagesToExcludeLemmas(), countPages);

            Set<String> lemmasFromUser = getLemmaFinder().getLemmaSet(query);
            Set<LemmaEntity> lemmasSorted = getLemmaSorted(lemmasFromUser, minCountPagesToExcludeLemmas, siteId);

            List<Relevance> relevanceList;
            if (lemmasSorted.size() == 0) {
                relevanceList = new ArrayList<>();
            } else {
                List<PageEntity> pageEntityList = filterPage(lemmasSorted);
                relevanceList = calcAndSortRelevance(pageEntityList, lemmasSorted);
            }

            SearchingResponse response = formingResponse(relevanceList, lemmasFromUser, offset, limit);
            responseServ.setSearchingResponse(response);
            responseServ.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            SearchingResponse response = formingResponseError(e.getLocalizedMessage());
            responseServ.setSearchingResponse(response);
            responseServ.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseServ;
    }
}
