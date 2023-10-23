package searchengine.config;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class LemmaConfiguration {
    /**
     * @return объект с русской морфологией
     * @throws IOException (если файл со словарем не найден)
     */
    @Bean
    public LuceneMorphology luceneMorphology() throws IOException {
        return new RussianLuceneMorphology();
    }

}
