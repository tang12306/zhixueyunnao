package com._1.service.impl;

import com._1.entity.Chapter;
import com._1.entity.Subject;
import com._1.repository.ChapterRepository;
import com._1.service.ChapterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class ChapterServiceImpl implements ChapterService {
    @Autowired
    private ChapterRepository chapterRepository;

    @Override
    public List<Chapter> findAll() {
        return chapterRepository.findAll();
    }

    @Override
    public List<Chapter> findBySubject(Subject subject) {
        return chapterRepository.findBySubjectOrderByOrderNumAsc(subject);
    }

    @Override
    public Optional<Chapter> findById(Long id) {
        return chapterRepository.findById(id);
    }

    @Override
    public Chapter save(Chapter chapter) {
        // 如果没有设置顺序号，则自动设置为当前科目下最大的顺序号+1
        if (chapter != null && chapter.getOrderNum() == null) {
            if (chapter.getSubject() != null) {
                List<Chapter> chapters = chapterRepository.findBySubjectOrderByOrderNumAsc(chapter.getSubject());
                int maxOrder = 0;
                if (chapters != null && !chapters.isEmpty()) {
                    for (Chapter ch : chapters) {
                        if (ch != null && ch.getOrderNum() != null && ch.getOrderNum() > maxOrder) {
                            maxOrder = ch.getOrderNum();
                        }
                    }
                }
                chapter.setOrderNum(maxOrder + 1);
            } else {
                // Handle case where chapter subject is null, perhaps log a warning or set a default orderNum
                // For now, if subject is null, orderNum will remain null if it was initially null.
            }
        }
        return chapterRepository.save(chapter);
    }

    @Override
    public void deleteById(Long id) {
        chapterRepository.deleteById(id);
    }

    @Override
    public boolean existsByNameAndSubject(String name, Subject subject) {
        return chapterRepository.existsByNameAndSubject(name, subject);
    }

    @Override
    public Optional<Chapter> findByNameAndSubject(String name, Subject subject) {
        return chapterRepository.findByNameAndSubject(name, subject);
    }
} 