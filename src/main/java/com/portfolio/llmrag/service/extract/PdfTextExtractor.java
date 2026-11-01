package com.portfolio.llmrag.service.extract;

import com.portfolio.llmrag.exception.TextExtractionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.encryption.InvalidPasswordException;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * PDF 페이지별 텍스트 추출 (PDFBox 3.x).
 */
@Component
public class PdfTextExtractor implements TextExtractor {

    @Override
    public boolean supports(String extension) {
        return ".pdf".equals(extension);
    }

    @Override
    public List<PageText> extract(Path path) {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true); // 2단 편집·표가 있는 문서의 글자 순서 보정

            List<PageText> pages = new ArrayList<>();
            int totalPages = document.getNumberOfPages();

            for (int pageNumber = 1; pageNumber <= totalPages; pageNumber++) {
                stripper.setStartPage(pageNumber);
                stripper.setEndPage(pageNumber);

                String text = TextCleaner.clean(stripper.getText(document));
                if (!text.isEmpty()) {
                    pages.add(new PageText(pageNumber, text));
                }
            }

            if (pages.isEmpty()) {
                throw new TextExtractionException(
                        "텍스트를 추출할 수 없습니다. 스캔(이미지) 문서일 수 있어 OCR이 필요합니다.");
            }
            return pages;

        } catch (InvalidPasswordException e) {
            throw new TextExtractionException("암호가 설정된 PDF는 처리할 수 없습니다.", e);
        } catch (IOException e) {
            throw new TextExtractionException("PDF를 읽을 수 없습니다. 파일이 손상되었을 수 있습니다.", e);
        }
    }
}
