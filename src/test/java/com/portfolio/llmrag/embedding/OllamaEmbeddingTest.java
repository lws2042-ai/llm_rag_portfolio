package com.portfolio.llmrag.embedding;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Ollama(bge-m3) 임베딩 연결 확인.
 * Ollama 가 localhost:11434 에서 실행 중일 때만 돌고, 아니면 건너뛴다.
 */
@SpringBootTest
@EnabledIf("ollamaRunning")
class OllamaEmbeddingTest {

    /** bge-m3 의 벡터 차원 */
    private static final int BGE_M3_DIMENSIONS = 1024;

    @Autowired
    EmbeddingModel embeddingModel;

    @Test
    void 한_문장을_1024차원_벡터로_바꾼다() {
        float[] vector = embeddingModel.embed("안녕하세요");

        assertEquals(BGE_M3_DIMENSIONS, vector.length);
        assertEquals(BGE_M3_DIMENSIONS, embeddingModel.dimensions());
    }

    @Test
    void 뜻이_비슷한_문장이_더_가깝다() {
        float[] question = embeddingModel.embed("출장비는 얼마까지 나와요?");
        float[] related = embeddingModel.embed("출장 경비 지급 한도는 1일 10만 원이다.");
        float[] unrelated = embeddingModel.embed("오늘 점심 메뉴 추천해 줘.");

        double relatedScore = cosine(question, related);
        double unrelatedScore = cosine(question, unrelated);

        assertTrue(relatedScore > unrelatedScore,
                "관련 문장 " + relatedScore + " 이 무관한 문장 " + unrelatedScore + " 보다 가까워야 한다");
    }

    private static double cosine(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    static boolean ollamaRunning() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("localhost", 11434), 500);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}
