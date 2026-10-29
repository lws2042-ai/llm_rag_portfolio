

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import com.portfolio.llmrag.service.PdfTextExtractor;

import org.junit.jupiter.api.Test;

class PdfTextExtractorTest {

    @Test
    void shouldExtractTextFromUploadedPdf() throws Exception {
        Path uploadDir = Path.of("uploads");

        Path pdfPath;

        try (var paths = Files.list(uploadDir)) {
            pdfPath = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString().toLowerCase()
                            .endsWith(".pdf"))
                    .max(Comparator.comparingLong(path -> {
                        try {
                            return Files.getLastModifiedTime(path).toMillis();
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }))
                    .orElseThrow(() ->
                            new IllegalStateException("테스트할 PDF 파일이 없습니다."));
        }

        PdfTextExtractor extractor = new PdfTextExtractor();
        String text = extractor.extract(pdfPath);

        assertFalse(text.isBlank(), "PDF에서 추출한 텍스트가 비어 있습니다.");

        System.out.println("테스트 파일: " + pdfPath.getFileName());
        System.out.println("추출 문자 수: " + text.length());
        System.out.println("텍스트 앞부분: "
                + text.substring(0, Math.min(200, text.length())));

        assertTrue(text.length() > 0);
    }
}