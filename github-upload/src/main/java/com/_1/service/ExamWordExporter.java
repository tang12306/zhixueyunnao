package com._1.service;

import com._1.dto.ai.ExamExportRequest;
import com._1.entity.QuestionType;
import com._1.service.ai.QuestionDraft;
import com._1.service.ai.QuestionTextNormalizer;
import org.apache.poi.xwpf.usermodel.BreakType;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 把试卷草稿导出为 Word：题目按题型分组，题号连续，参考答案与题号一一对应。
 */
@Component
public class ExamWordExporter {

    private static final String FONT = "宋体";
    private static final String[] SECTION_NUMBERS = {"一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};

    public byte[] export(ExamExportRequest request) throws IOException {
        Map<QuestionType, List<QuestionDraft>> sections = groupByType(request.getQuestions());

        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XWPFRun title = paragraph(document, ParagraphAlignment.CENTER).createRun();
            style(title, request.getName(), 18).setBold(true);

            String info = String.format("总分：%d分    时长：%d分钟    日期：%s",
                    request.getTotalScore() != null ? request.getTotalScore() : totalScore(request.getQuestions()),
                    request.getDuration() != null ? request.getDuration() : 120,
                    LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy年MM月dd日")));
            style(paragraph(document, ParagraphAlignment.LEFT).createRun(), info, 12);

            if (request.getDescription() != null && !request.getDescription().isBlank()) {
                style(paragraph(document, ParagraphAlignment.LEFT).createRun(), "说明：" + request.getDescription().trim(), 11);
            }
            style(paragraph(document, ParagraphAlignment.LEFT).createRun(), "—".repeat(40), 10);

            int sectionIndex = 0;
            int number = 1;
            for (Map.Entry<QuestionType, List<QuestionDraft>> section : sections.entrySet()) {
                List<QuestionDraft> questions = section.getValue();
                String heading = sectionNumber(sectionIndex++) + "、" + section.getKey().getLabel()
                        + "（共" + questions.size() + "题）";
                style(paragraph(document, ParagraphAlignment.LEFT).createRun(), heading, 14).setBold(true);

                for (QuestionDraft question : questions) {
                    String score = question.score() != null ? "（" + question.score() + "分）" : "";
                    multiline(style(paragraph(document, ParagraphAlignment.LEFT).createRun(), null, 12),
                            number++ + ". " + nullToEmpty(question.content()) + score);
                    List<String> options = QuestionTextNormalizer.stripLetterPrefixes(question.options());
                    for (int i = 0; i < options.size(); i++) {
                        style(paragraph(document, ParagraphAlignment.LEFT).createRun(),
                                "    " + (char) ('A' + i) + ". " + options.get(i), 11);
                    }
                    document.createParagraph();
                }
            }

            XWPFRun answerTitle = paragraph(document, ParagraphAlignment.CENTER).createRun();
            answerTitle.addBreak(BreakType.PAGE);
            style(answerTitle, "参考答案", 14).setBold(true);

            number = 1;
            for (List<QuestionDraft> questions : sections.values()) {
                for (QuestionDraft question : questions) {
                    multiline(style(paragraph(document, ParagraphAlignment.LEFT).createRun(), null, 11),
                            number++ + ". " + nullToEmpty(question.answer()));
                    if (question.analysis() != null && !question.analysis().isBlank()) {
                        XWPFRun analysis = style(paragraph(document, ParagraphAlignment.LEFT).createRun(), null, 10);
                        analysis.setColor("666666");
                        multiline(analysis, "    解析：" + question.analysis().trim());
                    }
                }
            }

            document.write(out);
            return out.toByteArray();
        }
    }

    /** 按题型的固定顺序分组；没有题型的题放到简答题 */
    private static Map<QuestionType, List<QuestionDraft>> groupByType(List<QuestionDraft> questions) {
        Map<QuestionType, List<QuestionDraft>> sections = new EnumMap<>(QuestionType.class);
        for (QuestionDraft question : questions) {
            if (question == null) {
                continue;
            }
            QuestionType type = question.type() != null ? question.type() : QuestionType.SHORT_ANSWER;
            sections.computeIfAbsent(type, k -> new ArrayList<>()).add(question);
        }
        return sections;
    }

    private static int totalScore(List<QuestionDraft> questions) {
        return questions.stream().filter(q -> q != null && q.score() != null).mapToInt(QuestionDraft::score).sum();
    }

    private static String sectionNumber(int index) {
        return index < SECTION_NUMBERS.length ? SECTION_NUMBERS[index] : String.valueOf(index + 1);
    }

    private static XWPFParagraph paragraph(XWPFDocument document, ParagraphAlignment alignment) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(alignment);
        return paragraph;
    }

    private static XWPFRun style(XWPFRun run, String text, int fontSize) {
        run.setFontFamily(FONT);
        run.setFontSize(fontSize);
        if (text != null) {
            run.setText(text);
        }
        return run;
    }

    /** Word 的 run 不认 \n，要逐行加换行符 */
    private static void multiline(XWPFRun run, String text) {
        String[] lines = text.split("\\R", -1);
        run.setText(lines[0]);
        for (int i = 1; i < lines.length; i++) {
            run.addBreak();
            run.setText(lines[i]);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
