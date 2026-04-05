import os

files = [
    r'c:\Users\USER\OneDrive\Desktop\SWT301\demo111\src\main\webapp\WEB-INF\user\blog\index.html',
    r'c:\Users\USER\OneDrive\Desktop\SWT301\demo111\src\main\webapp\WEB-INF\user\blog\detail.html',
    r'c:\Users\USER\OneDrive\Desktop\SWT301\demo111\src\main\webapp\WEB-INF\user\blog\create.html',
    r'c:\Users\USER\OneDrive\Desktop\SWT301\demo111\src\main\webapp\WEB-INF\user\blog\edit.html'
]

header_html = """<div class="top-bar">
    <div class="top-container">
        <th:block th:if="${account == null}">
            <a th:href="@{/login}" class="top-link">Đăng nhập</a>
            <span style="margin-left: 10px; opacity: 0.5;">|</span>
            <a th:href="@{/register}" class="top-link">Đăng ký</a>
        </th:block>
        <th:block th:if="${account != null}">
            <span style="color: white; font-size: 13px;">Xin chào, </span>
            <a th:href="@{/profile}" class="top-link" th:text="${account.name}" style="margin-left: 5px; font-weight: bold;">Tên User</a>
            <span style="margin-left: 10px; opacity: 0.5;">|</span>
            <a th:href="@{/logout}" class="top-link">Đăng xuất</a>
        </th:block>
        <img src="https://flagcdn.com/w40/gb.png" class="lang-icon" alt="English" style="margin-left: 15px;">
    </div>
</div>

<header class="header-main">
    <div class="header-container">
        <a th:href="@{/home}"><img src="https://www.betacinemas.vn/Assets/Common/logo/logo.png" class="logo"></a>
        <nav class="nav-menu">
            <a th:href="@{/home}">LỊCH CHIẾU THEO RẠP</a>
            <a th:href="@{/home}">PHIM</a>
            <a href="#">RẠP</a>
            <a href="#">GIÁ VÉ</a>
            <a th:href="@{/user/vouchers}">KHO VOUCHER</a>
            <a th:href="@{/blog}" style="color: #0054a6;">TIN TỨC</a>
        </nav>
    </div>
</header>
"""

footer_html = """<footer class="footer">
    <div class="footer-container">
        <div>
            <h4>BETA CINEMAS</h4>
            <p>Hệ thống rạp chiếu phim hàng đầu cho giới trẻ.</p>
        </div>
        <div>
            <h4>LIÊN HỆ</h4>
            <p>Hotline: 1900 636807</p>
        </div>
        <div>
            <h4>KẾT NỐI</h4>
            <p>Facebook | Youtube | TikTok</p>
        </div>
    </div>
</footer>
"""

css_append = """
        /* HEADER/FOOTER CSS */
        .top-bar { background: #0054a6; color: white; padding: 8px 0; font-size: 13px; }
        .top-container { max-width: 1200px; margin: 0 auto; display: flex; justify-content: flex-end; padding: 0 15px; }
        .top-link { color: white; text-decoration: none; margin-left: 10px; }
        .header-main { background: white; padding: 15px 0; box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05); position: sticky; top: 0; z-index: 1000; }
        .header-container { max-width: 1300px; margin: 0 auto; display: flex; align-items: center; padding: 0 15px; }
        .logo { height: 55px; margin-right: 25px; }
        .nav-menu { display: flex; justify-content: center; flex-grow: 1; gap: 25px; }
        .nav-menu a { text-decoration: none; color: #333; font-weight: 700; font-size: 16px; }
        .footer { background: #0054a6; color: white; padding: 50px 0; margin-top: auto; }
        .footer-container { max-width: 1200px; margin: 0 auto; display: grid; grid-template-columns: repeat(3, 1fr); gap: 50px; padding: 0 15px; }
"""

for fi in files:
    with open(fi, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Apply Light theme CSS Variables
    content = content.replace('--primary: #4F46E5', '--primary: #0054a6')
    content = content.replace('--bg: #0f172a', '--bg: #fdfdfd')
    content = content.replace('--card: rgba(255,255,255,0.05)', '--card: #ffffff')
    content = content.replace('--border: rgba(255,255,255,0.1)', '--border: #e2e8f0')
    
    # Change dark body block to light body block
    content = content.replace(
        "body { background: linear-gradient(135deg, #0f172a, #1e1b4b, #312e81); min-height: 100vh; color: #f8fafc; padding: 40px 20px; background-size: 400% 400%; animation: gradientBG 15s ease infinite; }",
        "body { background: #fdfdfd; min-height: 100vh; color: #333; display: flex; flex-direction: column; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }"
    )
    content = content.replace(
        "body { background: linear-gradient(135deg, #0f172a, #1e1b4b, #312e81); min-height: 100vh; display: flex; align-items: center; justify-content: center; padding: 40px 20px; background-size: 400% 400%; animation: gradientBG 15s ease infinite; color: #f8fafc; }",
        "body { background: #fdfdfd; min-height: 100vh; color: #333; display: flex; flex-direction: column; align-items: center; justify-content: flex-start; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; }\n.card { margin-top: 50px; flex-shrink: 0; margin-bottom: 50px; }"
    )
    
    content = content.replace("@keyframes gradientBG { 0%,100%{background-position:0% 50%} 50%{background-position:100% 50%} }", "")
    
    # Add Header HTML
    content = content.replace("<body>", "<body>\n" + header_html + "\n<div style=\"padding: 40px 20px; width: 100%; display: flex; justify-content: center; flex: 1;\">\n<div style=\"width: 100%; max-width: 800px;\">")
    
    # Find script block and add footer HTML
    if "<script" in content:
        content = content.replace("<script", "</div></div>\n" + footer_html + "\n<script", 1)
    else:
        content = content.replace("</body>", "</div></div>\n" + footer_html + "\n</body>")
        
    # Apply structural style overrides for light theme
    content = content.replace("color: #f8fafc", "color: #0f172a")
    content = content.replace("color: #cbd5e1", "color: #334155")
    content = content.replace("color: #94a3b8", "color: #64748b")
    content = content.replace("background: rgba(15,23,42,0.5)", "background: #f8fafc")
    content = content.replace("background: rgba(255,255,255,0.05)", "background: #ffffff")
    content = content.replace("border: 1px solid rgba(255,255,255,0.15)", "border: 1px solid #cbd5e1")
    content = content.replace("border: 1px solid rgba(255,255,255,0.1)", "border: 1px solid #e2e8f0")
    content = content.replace("background: rgba(15, 23, 42, 0.95)", "background: #ffffff")
    content = content.replace("backdrop-filter: blur(24px)", "box-shadow: 0 8px 20px rgba(0,0,0,0.06)")
    content = content.replace("backdrop-filter: blur(20px)", "box-shadow: 0 8px 20px rgba(0,0,0,0.06)")
    content = content.replace("box-shadow: 0 10px 40px rgba(0,0,0,0.5)", "box-shadow: 0 8px 30px rgba(0,0,0,0.15)")
    
    # Append Header/Footer CSS
    content = content.replace("</style>", css_append + "\n</style>")
    
    with open(fi, 'w', encoding='utf-8') as f:
         f.write(content)
    print(f'Updated {fi}')
