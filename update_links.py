import os

folder = r"c:\Users\USER\OneDrive\Desktop\SWT301\demo111\src\main\webapp\WEB-INF\admin"
print("Scanning " + folder)
for f in os.listdir(folder):
    if f.endswith(".html"):
        path = os.path.join(folder, f)
        with open(path, "r", encoding="utf-8") as file:
            c = file.read()
            
        nc = c.replace('<a th:href="@{/admin/manager_news}">', '<a th:href="@{/admin/manager_news}" style="text-decoration: none;">')
        nc = nc.replace('<a th:href="@{/admin/manager_vouchers}">', '<a th:href="@{/admin/manager_vouchers}" style="text-decoration: none;">')
        
        if c != nc:
            with open(path, "w", encoding="utf-8") as file:
                file.write(nc)
            print("Updated " + f)
print("Done")
