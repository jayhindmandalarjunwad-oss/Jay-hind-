import http.server
import socketserver
import os
import subprocess
import cgi
import sys

PORT = 3000
BASE_DIR = os.path.dirname(os.path.abspath(__file__))

HTML_FORM = """<!DOCTYPE html>
<html lang="mr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>जय हिंद मंडळ - लोगो अपडेट पोर्टल</title>
    <style>
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            background: #F8F9FA;
            color: #1A1A1A;
            display: flex;
            justify-content: center;
            align-items: center;
            min-height: 100vh;
            padding: 20px;
        }
        .card {
            background: #FFFFFF;
            border-radius: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.1);
            max-width: 460px;
            width: 100%;
            padding: 28px 24px;
            text-align: center;
        }
        .header-badge {
            display: inline-block;
            background: #FFF4E5;
            color: #D35400;
            padding: 6px 16px;
            border-radius: 20px;
            font-size: 13px;
            font-weight: 700;
            margin-bottom: 16px;
        }
        h1 {
            font-size: 22px;
            font-weight: 800;
            color: #1E293B;
            margin-bottom: 8px;
        }
        p {
            font-size: 14px;
            color: #64748B;
            line-height: 1.5;
            margin-bottom: 24px;
        }
        .preview-box {
            width: 150px;
            height: 150px;
            border-radius: 50%;
            border: 3px solid #D35400;
            margin: 0 auto 24px;
            overflow: hidden;
            background: #FFF;
            display: flex;
            align-items: center;
            justify-content: center;
            box-shadow: 0 4px 15px rgba(211, 84, 0, 0.2);
        }
        .preview-box img {
            width: 100%;
            height: 100%;
            object-fit: contain;
        }
        .file-input-wrapper {
            margin-bottom: 20px;
        }
        input[type="file"] {
            display: none;
        }
        .custom-file-btn {
            display: block;
            border: 2px dashed #CBD5E1;
            border-radius: 12px;
            padding: 16px;
            cursor: pointer;
            color: #475569;
            font-size: 14px;
            font-weight: 600;
            transition: all 0.2s;
            background: #F8FAFC;
        }
        .custom-file-btn:hover {
            border-color: #D35400;
            background: #FFF4E5;
            color: #D35400;
        }
        .submit-btn {
            background: linear-gradient(135deg, #D35400, #E67E22);
            color: white;
            border: none;
            border-radius: 12px;
            padding: 14px 28px;
            font-size: 16px;
            font-weight: 700;
            cursor: pointer;
            width: 100%;
            box-shadow: 0 6px 18px rgba(211, 84, 0, 0.35);
            transition: all 0.2s;
        }
        .submit-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 8px 22px rgba(211, 84, 0, 0.45);
        }
        .alert {
            padding: 14px;
            border-radius: 12px;
            margin-bottom: 20px;
            font-size: 14px;
            font-weight: 600;
        }
        .alert-success {
            background: #ECFDF5;
            color: #065F46;
            border: 1px solid #A7F3D0;
        }
        .alert-error {
            background: #FEF2F2;
            color: #991B1B;
            border: 1px solid #FECACA;
        }
        .status-list {
            text-align: left;
            margin-top: 16px;
            font-size: 13px;
            color: #334155;
            line-height: 1.8;
            padding-left: 20px;
        }
    </style>
</head>
<body>
    <div class="card">
        <div class="header-badge">जय हिंद मंडळ अर्जुनवाड</div>
        <h1>लोगो अपडेट पोर्टल</h1>
        <p>नवीन लोगोची इमेज निवडून खालील बटणावर क्लिक करा. संपूर्ण ॲपमध्ये (ड्रॉवर, आयडी कार्ड, ॲप आयकॉन) लोगो १००% अपडेट होईल.</p>

        {ALERT_BLOCK}

        <div class="preview-box">
            <img id="logo-preview" src="/current_logo" alt="Current Logo" onerror="this.src='data:image/svg+xml;utf8,<svg xmlns=\\'http://www.w3.org/2000/svg\\' width=\\'100\\' height=\\'100\\'><text x=\\'50%\\' y=\\'50%\\' text-anchor=\\'middle\\' fill=\\'%23999\\' dy=\\'.3em\\'>LOGO</text></svg>'">
        </div>

        <form method="POST" enctype="multipart/form-data">
            <div class="file-input-wrapper">
                <label for="logo_file" class="custom-file-btn" id="file-label">
                    📁 नवीन लोगो निवडा (Choose Logo Image)
                </label>
                <input type="file" name="logo_file" id="logo_file" accept="image/*" required onchange="updateFileName(this)">
            </div>
            <button type="submit" class="submit-btn">सर्वत्र लोगो बदला (Update Everywhere)</button>
        </form>
    </div>

    <script>
        function updateFileName(input) {
            const label = document.getElementById('file-label');
            const preview = document.getElementById('logo-preview');
            if (input.files && input.files[0]) {
                label.textContent = "✓ निवडलेली फाईल: " + input.files[0].name;
                label.style.borderColor = "#D35400";
                label.style.color = "#D35400";
                const reader = new FileReader();
                reader.onload = function(e) {
                    preview.src = e.target.result;
                };
                reader.readAsDataURL(input.files[0]);
            }
        }
    </script>
</body>
</html>
"""

def update_all_logos(temp_path):
    targets = [
        ("app/src/main/res/drawable/ic_jayhind_logo.jpg", None),
        ("pwa/icons/icon-512.jpg", "512x512"),
        ("pwa/icons/icon-512.png", "512x512"),
        ("pwa/icons/icon-192.png", "192x192"),
        ("app/src/main/res/mipmap-mdpi/ic_launcher.png", "48x48"),
        ("app/src/main/res/mipmap-mdpi/ic_launcher_round.png", "48x48"),
        ("app/src/main/res/mipmap-hdpi/ic_launcher.png", "72x72"),
        ("app/src/main/res/mipmap-hdpi/ic_launcher_round.png", "72x72"),
        ("app/src/main/res/mipmap-xhdpi/ic_launcher.png", "96x96"),
        ("app/src/main/res/mipmap-xhdpi/ic_launcher_round.png", "96x96"),
        ("app/src/main/res/mipmap-xxhdpi/ic_launcher.png", "144x144"),
        ("app/src/main/res/mipmap-xxhdpi/ic_launcher_round.png", "144x144"),
        ("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png", "192x192"),
        ("app/src/main/res/mipmap-xxxhdpi/ic_launcher_round.png", "192x192"),
    ]

    for rel_path, size in targets:
        full_path = os.path.join(BASE_DIR, rel_path)
        os.makedirs(os.path.dirname(full_path), exist_ok=True)
        if size:
            cmd = ["convert", temp_path, "-resize", size, full_path]
        else:
            cmd = ["convert", temp_path, full_path]
        subprocess.run(cmd, check=True)

class UploadHandler(http.server.BaseHTTPRequestHandler):
    def do_HEAD(self):
        if self.path == "/current_logo":
            self.send_response(200)
            self.send_header("Content-Type", "image/jpeg")
            self.end_headers()
        else:
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()

    def do_GET(self):
        if self.path == "/current_logo":
            logo_path = os.path.join(BASE_DIR, "app/src/main/res/drawable/ic_jayhind_logo.jpg")
            if os.path.exists(logo_path):
                self.send_response(200)
                self.send_header("Content-Type", "image/jpeg")
                self.send_header("Cache-Control", "no-cache, must-revalidate")
                self.end_headers()
                with open(logo_path, "rb") as f:
                    self.wfile.write(f.read())
                return
            else:
                self.send_error(404)
                return

        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        html = HTML_FORM.replace("{ALERT_BLOCK}", "")
        self.wfile.write(html.encode("utf-8"))

    def do_POST(self):
        ctype, pdict = cgi.parse_header(self.headers.get("content-type"))
        if ctype == "multipart/form-data":
            pdict["boundary"] = bytes(pdict["boundary"], "utf-8")
            fields = cgi.parse_multipart(self.rfile, pdict)
            file_data = fields.get("logo_file")
            if file_data and len(file_data) > 0 and len(file_data[0]) > 0:
                temp_path = "/tmp/new_uploaded_logo.jpg"
                with open(temp_path, "wb") as f:
                    f.write(file_data[0])

                try:
                    update_all_logos(temp_path)
                    with open("/tmp/logo_updated.flag", "w") as f:
                        f.write("UPDATED")

                    alert = """
                    <div class="alert alert-success">
                        🎉 नवीन लोगो यशस्वीरित्या सेव्ह झाला!<br>
                        ड्रॉवर, आयडी कार्ड आणि सर्व ॲप आयकॉन १००% अपडेट झाले आहेत.
                    </div>
                    """
                    self.send_response(200)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    html = HTML_FORM.replace("{ALERT_BLOCK}", alert)
                    self.wfile.write(html.encode("utf-8"))
                    return
                except Exception as e:
                    alert = f"""
                    <div class="alert alert-error">
                        त्रुटी: {str(e)}
                    </div>
                    """
                    self.send_response(500)
                    self.send_header("Content-Type", "text/html; charset=utf-8")
                    self.end_headers()
                    html = HTML_FORM.replace("{ALERT_BLOCK}", alert)
                    self.wfile.write(html.encode("utf-8"))
                    return

        self.send_response(400)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        alert = """
        <div class="alert alert-error">
            कृपया वैध इमेज फाईल निवडा!
        </div>
        """
        html = HTML_FORM.replace("{ALERT_BLOCK}", alert)
        self.wfile.write(html.encode("utf-8"))

class ReusableThreadingServer(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

if __name__ == '__main__':
    server = ReusableThreadingServer(('0.0.0.0', PORT), UploadHandler)
    print(f'Serving upload portal on port {PORT}')
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        server.server_close()
