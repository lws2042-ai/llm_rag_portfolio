package com.portfolio.llmrag.service.extract;

import com.portfolio.llmrag.exception.TextExtractionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PdfTextExtractorTest {

    private final PdfTextExtractor extractor = new PdfTextExtractor();

    @TempDir
    Path tempDir;

    @Test
    void 페이지별로_추출하고_빈_페이지는_건너뛴다() throws IOException {
        Path pdf = tempDir.resolve("sample.pdf");
        createPdf(pdf, "First page", null, "Third page");

        List<PageText> pages = extractor.extract(pdf);

        assertEquals(2, pages.size());
        assertEquals(1, pages.get(0).pageNumber());
        assertTrue(pages.get(0).text().contains("First page"));
        assertEquals(3, pages.get(1).pageNumber()); // 2쪽은 비어 있어 제외, 번호는 원본 유지
        assertTrue(pages.get(1).text().contains("Third page"));
    }

    @Test
    void 텍스트가_없는_PDF는_스캔본으로_보고_실패한다() throws IOException {
        Path pdf = tempDir.resolve("scanned.pdf");
        createPdf(pdf, (String) null);

        TextExtractionException e = assertThrows(TextExtractionException.class, () -> extractor.extract(pdf));
        assertTrue(e.getMessage().contains("OCR"));
    }

    @Test
    void 손상된_파일은_읽기_실패로_처리한다() throws IOException {
        Path broken = tempDir.resolve("broken.pdf");
        Files.writeString(broken, "%PDF-1.7 this is not a real pdf");

        assertThrows(TextExtractionException.class, () -> extractor.extract(broken));
    }

    /** 페이지마다 한 줄씩 쓴 PDF 를 만든다. null 이면 빈 페이지. */
    private static void createPdf(Path path, String... pageTexts) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            for (String text : pageTexts) {
                PDPage page = new PDPage();
                document.addPage(page);
                if (text == null) {
                    continue;
                }
                try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                    content.beginText();
                    content.setFont(font, 12);
                    content.newLineAtOffset(50, 700);
                    content.showText(text);
                    content.endText();
                }
            }
            document.save(path.toFile());
        }
    }
}
