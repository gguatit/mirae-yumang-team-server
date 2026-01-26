package com.example.demo.crawler;

import com.example.demo.entity.Unsae;
// import com.example.demo.repository.UnsaeRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CrawlingUtils {

    /**
     * 지정한 URL의 "#seiza-area" 내부 .seiza-box 항목 전체를 읽어 Unsae 리스트로 변환
     * - 각 .seiza-box 내부의 클래스 구조를 기준으로 필드 추출
     */
    public static List<Unsae> crawlUnsae(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .timeout(10_000)
                .get();

        Element seizaArea = doc.selectFirst(".seiza-area");
        if (seizaArea == null) return Collections.emptyList();

        Elements boxes = seizaArea.select(".seiza-box");
        List<Unsae> results = new ArrayList<>(boxes.size());

        for (int i = 0; i < boxes.size(); i++) {
            Element box = boxes.get(i);
            String con = textOr(box.selectFirst(".seiza-txt"));
            String date = textOr(box.selectFirst(".period"));
            String content = textOr(box.selectFirst(".read"));
            String lCor = textOr(box.selectFirst(".lucky-color-txt"));
            String lKey = textOr(box.selectFirst(".key-txt"));

            // 아이콘 수를 카운트해서 문자열로 저장 (엔티티 필드가 String 형태라서)
            String money = String.valueOf(box.select(".icon-money").size());
            String love = String.valueOf(box.select(".icon-love").size());
            String study = String.valueOf(box.select(".icon-work").size());
            String health = String.valueOf(box.select(".icon-health").size());

            Unsae u = Unsae.builder()
                    .id(i + 1)
                    .con(con)
                    .date(date)
                    .content(content)
                    .lCor(lCor)
                    .lKey(lKey)
                    .money(money)
                    .love(love)
                    .study(study)
                    .health(health)
                    .build();

            results.add(u);
        }

        return results;
    }

    // /**
    //  * 크롤링 후 저장 (UnsaeRepository 전달)
    //  */
    // public static void crawlAndSave(String url, UnsaeRepository repo) throws IOException {
    //     List<Unsae> list = crawlUnsae(url);
    //     if (!list.isEmpty()) {
    //         repo.saveAll(list);
    //     }
    // }

    private static String textOr(Element e) {
        return e == null ? "" : e.text().trim();
    }
}