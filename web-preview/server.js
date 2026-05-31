const http = require("http");
const fs = require("fs");
const path = require("path");

const root = __dirname;
const port = 5177;

http.createServer((request, response) => {
  const url = request.url === "/" ? "/index.html" : request.url;
  const filePath = path.join(root, path.normalize(url).replace(/^(\.\.[/\\])+/, ""));

  fs.readFile(filePath, (error, data) => {
    if (error) {
      response.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
      response.end("Not found");
      return;
    }

    const ext = path.extname(filePath);
    const type = ext === ".html" ? "text/html; charset=utf-8" : "text/plain; charset=utf-8";
    response.writeHead(200, { "Content-Type": type });
    response.end(data);
  });
}).listen(port, "127.0.0.1", () => {
  console.log(`Tononkira preview: http://127.0.0.1:${port}`);
});
