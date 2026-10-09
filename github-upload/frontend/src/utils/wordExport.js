/**
 * Word导出工具类
 * 用于将试卷内容导出为Word文档
 */

/**
 * 生成Word文档内容
 * @param {Object} examPreview - 试卷预览数据
 * @param {Array} sections - 试卷分组数据
 * @returns {string} HTML格式的Word文档内容
 */
export function generateWordContent(examPreview, sections) {
  let content = '';
  
  // HTML文档开始
  content += '<html xmlns:o="urn:schemas-microsoft-com:office:office" ';
  content += 'xmlns:w="urn:schemas-microsoft-com:office:word" ';
  content += 'xmlns="http://www.w3.org/TR/REC-html40">';
  content += '<head>';
  content += '<meta charset="utf-8">';
  content += '<title>' + (examPreview.name || '试卷') + '</title>';
  
  // 添加样式
  content += '<style>';
  content += 'body { font-family: "Microsoft YaHei", Arial, sans-serif; margin: 20px; line-height: 1.6; }';
  content += '.exam-header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #333; padding-bottom: 20px; }';
  content += '.exam-title { font-size: 24px; font-weight: bold; margin-bottom: 10px; }';
  content += '.exam-info { font-size: 14px; color: #666; margin-bottom: 5px; }';
  content += '.section { margin-bottom: 30px; }';
  content += '.section-title { font-size: 18px; font-weight: bold; margin-bottom: 15px; }';
  content += '.question { margin-bottom: 20px; }';
  content += '.question-header { font-weight: bold; margin-bottom: 8px; }';
  content += '.question-content { margin-bottom: 10px; }';
  content += '.question-options { margin-left: 20px; }';
  content += '.question-option { margin-bottom: 5px; }';
  content += '</style>';
  content += '</head>';
  content += '<body>';
  
  // 试卷头部
  content += '<div class="exam-header">';
  content += '<div class="exam-title">' + (examPreview.name || '试卷') + '</div>';
  content += '<div class="exam-info">';
  content += '考试时间：' + (examPreview.duration || 120) + '分钟';
  content += '</div>';
  content += '<div class="exam-info">';
  content += '总分：' + (examPreview.totalScore || 100) + '分';
  content += '</div>';
  if (examPreview.description) {
    content += '<div class="exam-info">';
    content += examPreview.description;
    content += '</div>';
  }
  content += '</div>';
  
  // 试卷内容
  sections.forEach(section => {
    content += '<div class="section">';
    content += '<div class="section-title">' + section.title + '</div>';
    
    section.questions.forEach((question, index) => {
      content += '<div class="question">';
      content += '<div class="question-header">' + (index + 1) + '. (' + question.score + '分)</div>';
      content += '<div class="question-content">' + question.content + '</div>';
      
      if (question.options && question.options.length > 0) {
        content += '<div class="question-options">';
        question.options.forEach(option => {
          content += '<div class="question-option">' + option + '</div>';
        });
        content += '</div>';
      }
      
      content += '</div>';
    });
    
    content += '</div>';
  });
  
  content += '</body></html>';
  return content;
}

/**
 * 导出Word文档
 * @param {Object} examPreview - 试卷预览数据
 * @param {Array} sections - 试卷分组数据
 * @returns {Promise} 导出结果
 */
export function exportToWord(examPreview, sections) {
  return new Promise((resolve, reject) => {
    try {
      const content = generateWordContent(examPreview, sections);
      const blob = new Blob([content], { 
        type: 'application/msword' 
      });
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = (examPreview.name || '试卷') + '.doc';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      resolve('Word文档导出成功');
    } catch (error) {
      reject(error);
    }
  });
}

/**
 * 生成答题卡Word文档内容
 * @param {Object} examPreview - 试卷预览数据
 * @param {Array} sections - 试卷分组数据
 * @returns {string} HTML格式的答题卡内容
 */
export function generateAnswerSheetContent(examPreview, sections) {
  let content = '';
  
  // HTML文档开始
  content += '<html xmlns:o="urn:schemas-microsoft-com:office:office" ';
  content += 'xmlns:w="urn:schemas-microsoft-com:office:word" ';
  content += 'xmlns="http://www.w3.org/TR/REC-html40">';
  content += '<head>';
  content += '<meta charset="utf-8">';
  content += '<title>' + (examPreview.name || '试卷') + ' - 答题卡</title>';
  
  // 添加样式
  content += '<style>';
  content += 'body { font-family: "Microsoft YaHei", Arial, sans-serif; margin: 20px; line-height: 1.6; }';
  content += '.exam-header { text-align: center; margin-bottom: 30px; border-bottom: 2px solid #333; padding-bottom: 20px; }';
  content += '.exam-title { font-size: 24px; font-weight: bold; margin-bottom: 10px; }';
  content += '.exam-info { font-size: 14px; color: #666; margin-bottom: 5px; }';
  content += '.student-info { margin-bottom: 30px; }';
  content += '.info-line { margin-bottom: 10px; }';
  content += '.section { margin-bottom: 30px; }';
  content += '.section-title { font-size: 18px; font-weight: bold; margin-bottom: 15px; }';
  content += '.answer-area { margin-bottom: 20px; }';
  content += '.answer-line { border-bottom: 1px solid #ccc; height: 30px; margin-bottom: 10px; }';
  content += '</style>';
  content += '</head>';
  content += '<body>';
  
  // 试卷头部
  content += '<div class="exam-header">';
  content += '<div class="exam-title">' + (examPreview.name || '试卷') + ' - 答题卡</div>';
  content += '<div class="exam-info">';
  content += '考试时间：' + (examPreview.duration || 120) + '分钟';
  content += '</div>';
  content += '<div class="exam-info">';
  content += '总分：' + (examPreview.totalScore || 100) + '分';
  content += '</div>';
  content += '</div>';
  
  // 学生信息
  content += '<div class="student-info">';
  content += '<div class="info-line">姓名：___________________ 学号：___________________</div>';
  content += '<div class="info-line">班级：___________________ 日期：___________________</div>';
  content += '</div>';
  
  // 答题区域
  sections.forEach(section => {
    content += '<div class="section">';
    content += '<div class="section-title">' + section.title + '</div>';
    
    section.questions.forEach((question, index) => {
      content += '<div class="answer-area">';
      content += '<div>' + (question.number || index + 1) + '. (' + question.score + '分)</div>';
      
      // 根据题目类型生成不同的答题区域
      if (['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'TRUE_FALSE'].includes(question.type)) {
        content += '<div>答案：___________</div>';
      } else if (question.type === 'FILL_IN_THE_BLANK') {
        content += '<div class="answer-line"></div>';
      } else {
        // 简答题等需要更多空间
        content += '<div class="answer-line"></div>';
        content += '<div class="answer-line"></div>';
        content += '<div class="answer-line"></div>';
      }
      
      content += '</div>';
    });
    
    content += '</div>';
  });
  
  content += '</body></html>';
  return content;
}

/**
 * 导出答题卡Word文档
 * @param {Object} examPreview - 试卷预览数据
 * @param {Array} sections - 试卷分组数据
 * @returns {Promise} 导出结果
 */
export function exportAnswerSheet(examPreview, sections) {
  return new Promise((resolve, reject) => {
    try {
      const content = generateAnswerSheetContent(examPreview, sections);
      const blob = new Blob([content], { 
        type: 'application/msword' 
      });
      
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = (examPreview.name || '试卷') + '_答题卡.doc';
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      window.URL.revokeObjectURL(url);
      
      resolve('答题卡导出成功');
    } catch (error) {
      reject(error);
    }
  });
}
