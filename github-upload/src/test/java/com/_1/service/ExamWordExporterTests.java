package com._1.service;

import com._1.dto.ai.ExamExportRequest;
import com._1.entity.QuestionType;
import com._1.service.ai.QuestionDraft;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExamWordExporterTests {

    @Test
    void groupsByTypeAndKeepsAnswersAlignedWithNumbers() throws Exception {
        ExamExportRequest request = new ExamExportRequest();
        request.setName("计算机网络期中考试");
        request.setDuration(90);
        request.setQuestions(List.of(
                new QuestionDraft(QuestionType.SHORT_ANSWER, "简述三次握手。\n要求写出报文标志位。", List.of(), "SYN、SYN+ACK、ACK", "", 10),
                new QuestionDraft(QuestionType.SINGLE_CHOICE, "TCP 属于哪一层？",
                        List.of("A. 应用层", "B. 传输层"), "B", "TCP 是传输层协议", 2),
                new QuestionDraft(QuestionType.TRUE_FALSE, "UDP 提供可靠传输。", List.of(), "错误", null, 2),
                new QuestionDraft(QuestionType.FILL_IN_THE_BLANK, "HTTP 默认端口是 ____。", List.of(), "80", null, 2)));

        byte[] bytes = new ExamWordExporter().export(request);

        String text;
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            text = extractor.getText();
        }

        assertThat(text).contains("计算机网络期中考试", "总分：16分", "时长：90分钟");
        assertThat(text).containsSubsequence(
                "一、单选题（共1题）", "1. TCP 属于哪一层？（2分）", "A. 应用层", "B. 传输层",
                "二、判断题（共1题）", "2. UDP 提供可靠传输。",
                "三、填空题（共1题）", "3. HTTP 默认端口是 ____。",
                "四、简答题（共1题）", "4. 简述三次握手。", "要求写出报文标志位。",
                "参考答案", "1. B", "解析：TCP 是传输层协议", "2. 错误", "3. 80", "4. SYN、SYN+ACK、ACK");
        // 选项不会出现两次字母前缀
        assertThat(text).doesNotContain("A. A.");
    }
}
