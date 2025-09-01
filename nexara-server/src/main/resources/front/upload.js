/* ========== 全局配置 ========== */
const BASE_URL = "http://127.0.0.1:8081/server/upload";

/* ========== 工具函数 ========== */
const $   = sel => document.querySelector(sel);
const log = msg => $("#log").textContent += msg + "\n";
const sleep = ms => new Promise(r => setTimeout(r, ms));

let task = {
    file: null, hash: "", chunkSize: 0, totalChunks: 0, uploaded: 0, paused: false
};

/* ========== 计算 MD5 ========== */
async function calculateHash(file) {
    return new Promise((resolve, reject) => {
        const spark = new SparkMD5.ArrayBuffer();
        const reader = new FileReader();
        reader.readAsArrayBuffer(file);
        reader.onload  = e => { spark.append(e.target.result); resolve(spark.end()); };
        reader.onerror = reject;
    });
}

/* ========== 网络封装 ========== */
async function request(url, method = "GET", body = null) {
    const opts = { method };
    if (body) opts.body = body;
    const res = await fetch(url, opts);
    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

/* ========== 初始化任务 ========== */
async function initTask() {
    const { file, chunkSize } = task;
    task.hash        = await calculateHash(file);
    task.totalChunks = Math.ceil(file.size / chunkSize);

    const params = new URLSearchParams({
        fileHash: task.hash, fileName: file.name,
        totalChunks: task.totalChunks, chunkSize: chunkSize / 1024
    });
    await request(`${BASE_URL}/init?${params}`, "POST");
    log(`文件大小：${Math.round(file.size / 1024)} KB`);
    log("初始化成功，开始上传...");
    renderProgress();
}

/* ========== 轮询断点续传状态 ========== */
async function fetchStatus() {
    const { data } = await request(`${BASE_URL}/status?fileHash=${task.hash}`);
    if (data && data.uploadedChunks) {
        task.uploaded = data.uploadedChunks.length;
    }
    renderProgress();
}

/* ========== 渲染进度 ========== */
function renderProgress() {
    $("#fileHash").textContent      = task.hash;
    $("#totalSize").textContent     = (task.file.size / 1024 / 1024).toFixed(2) + " MB";
    $("#totalChunks").textContent   = task.totalChunks;
    $("#uploadedChunks").textContent= task.uploaded;
    const percent = Math.round((task.uploaded / task.totalChunks) * 100);
    $("#percent").textContent = percent;
    $("#barInner").style.width = percent + "%";
}
/* ========== 滑动窗口上传（单片失败立即终止并打印） ========== */
const CHUNK_PER_BATCH = 4;

async function uploadWindow(startIdx, batchSize) {
    if (task.paused) return;

    for (let offset = 0; offset < batchSize; offset++) {
        const idx = startIdx + offset;
        if (idx >= task.totalChunks) break;

        let retries = 0;
        while (retries < 3) {
            try {
                const start = idx * task.chunkSize;
                const end   = Math.min(task.file.size, start + task.chunkSize);
                const blob  = task.file.slice(start, end);

                const form = new FormData();
                form.append("fileHash", task.hash);
                form.append("chunks", blob, `${task.file.name}_${idx}`);

                await request(`${BASE_URL}/chunk-batch`, "POST", form);
                task.uploaded++;
                renderProgress();
                break;                 // 单片成功
            } catch (e) {
                retries++;
                log(`[批 ${Math.floor(startIdx / CHUNK_PER_BATCH)}] 片 ${idx} 第 ${retries}/3 次失败：${e.message}`);
                await sleep(Math.pow(2, retries) * 1000);
            }
        }
        if (retries === 3) {
            // 单片重试耗尽，立即终止并打印
            log(`[批 ${Math.floor(startIdx / CHUNK_PER_BATCH)}] 片 ${idx} 最终失败，上传终止`);
            throw new Error(`[批 ${Math.floor(startIdx / CHUNK_PER_BATCH)}] 片 ${idx} 上传失败`);
        }
    }
}

/* ========== 主循环：单片失败即整体失败 ========== */
async function startUpload() {
    $("#progressBox").style.display = "block";
    await fetchStatus();   // 断点续传
    task.paused = false;

    try {
        for (let idx = task.uploaded; idx < task.totalChunks; ) {
            if (task.paused) break;
            const batchSize = Math.min(CHUNK_PER_BATCH, task.totalChunks - idx);
            await uploadWindow(idx, batchSize);
            idx += batchSize;
        }
        if (!task.paused && task.uploaded === task.totalChunks) {
            log("全部分片上传完成！");
        }
    } catch (e) {
        // 把异常真正抛到消息框
        log("上传失败：" + e.message);
    }
}

/* ========== 暂停 / 继续 ========== */
$("#pauseBtn").addEventListener("click", () => {
    task.paused = !task.paused;
    $("#pauseBtn").textContent = task.paused ? "继续" : "暂停";
    if (!task.paused) startUpload();
});

/* ========== 表单提交 ========== */
$("#uploadForm").addEventListener("submit", async e => {
    e.preventDefault();
    const file = $("#fileInput").files[0];
    if (!file) return alert("请选择文件");
    const chunkSize = Number($("#chunkSize").value) * 1024;

    task = { file, chunkSize, uploaded: 0 };
    $("#pauseBtn").disabled = false;
    log("开始计算文件 Hash...");
    try {
        await initTask();
        await startUpload();
    } catch (err) {
        log("初始化失败：" + err.message);
    }
});

/* ========== 引入 SparkMD5 ========== */
const script = document.createElement("script");
script.src = "https://cdn.jsdelivr.net/npm/spark-md5@3.0.2/spark-md5.min.js";
document.head.appendChild(script);