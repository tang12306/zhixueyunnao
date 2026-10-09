/*
 * 旧版 Thymeleaf 页面用：给同源的 fetch / jQuery 写请求（POST、PUT、DELETE 等）
 * 自动带上 CSRF 令牌。令牌由后端写在 XSRF-TOKEN Cookie 里。
 * 表单提交不需要它，th:action 会自动插入 _csrf 隐藏字段。
 */
(function () {
  var SAFE_METHODS = /^(GET|HEAD|OPTIONS|TRACE)$/i;

  function csrfToken() {
    var match = document.cookie.match(/(?:^|;\s*)XSRF-TOKEN=([^;]*)/);
    return match ? decodeURIComponent(match[1]) : null;
  }

  function isSameOrigin(url) {
    try {
      return new URL(url, window.location.href).origin === window.location.origin;
    } catch (e) {
      return false;
    }
  }

  if (window.fetch) {
    var originalFetch = window.fetch;
    window.fetch = function (input, init) {
      var isRequest = typeof Request !== 'undefined' && input instanceof Request;
      var method = (init && init.method) || (isRequest ? input.method : 'GET');
      var url = isRequest ? input.url : String(input);
      var token = csrfToken();
      if (token && !SAFE_METHODS.test(method) && isSameOrigin(url)) {
        var headers = new Headers((init && init.headers) || (isRequest ? input.headers : undefined));
        if (!headers.has('X-XSRF-TOKEN')) {
          headers.set('X-XSRF-TOKEN', token);
        }
        init = Object.assign({}, init, { headers: headers });
      }
      return originalFetch.call(this, input, init);
    };
  }

  // jQuery 通常在页面底部才加载，等 DOM 解析完再挂钩子
  document.addEventListener('DOMContentLoaded', function () {
    if (!window.jQuery) {
      return;
    }
    window.jQuery.ajaxPrefilter(function (options, originalOptions, xhr) {
      var token = csrfToken();
      if (token && !SAFE_METHODS.test(options.type || 'GET') && isSameOrigin(options.url)) {
        xhr.setRequestHeader('X-XSRF-TOKEN', token);
      }
    });
  });
})();
