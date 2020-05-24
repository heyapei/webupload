package com.github.zyt.webuploader.contorller;

import ch.qos.logback.classic.Logger;
import com.github.zyt.webuploader.bean.Res;
import com.github.zyt.webuploader.mapper.ResMapper;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 分片上传Controller
 */
@Controller
@RequestMapping("upload")
public class UploadController {

    private static Logger logger = (Logger) LoggerFactory.getLogger(UploadController.class);

    @Autowired
    private ResMapper resMapper;

    /**
     * 上传路径
     */
    private static String uploadPath = "D:/upload";


    @RequestMapping(value = "/check", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> check(@RequestParam("fileMD5") String md5) {
        logger.info("传入的文件md5数据是：" + md5);
        Res res = resMapper.selectByMD5(md5);
        Map<String, Object> map = new HashMap<String, Object>();
        if (res != null) {
            map.put("msg", "已存在");
            map.put("exist", true);
        } else {
            map.put("msg", "未存在");
            map.put("exist", false);
        }
        return map;
    }

    /**
     * 跳转到首页
     *
     * @return
     */
    @GetMapping("index")
    public String toUpload() {
        return "/upload";
    }

    /**
     * 查看当前分片是否上传
     *
     * @param request
     * @param response
     */
    @PostMapping("checkblock")
    @ResponseBody
    public void checkMd5(HttpServletRequest request, HttpServletResponse response) {
        //当前分片
        String chunk = request.getParameter("chunk");
        //分片大小
        String chunkSize = request.getParameter("chunkSize");
        //当前文件的MD5值
        String guid = request.getParameter("guid");
        //分片上传路径
        String tempPath = uploadPath + File.separator + "temp";
        File checkFile = new File(tempPath + File.separator + guid + File.separator + chunk);
        response.setContentType("text/html;charset=utf-8");
        try {
            //如果当前分片存在，并且长度等于上传的大小
            if (checkFile.exists() && checkFile.length() == Integer.parseInt(chunkSize)) {
                response.getWriter().write("{\"ifExist\":1}");
            } else {
                response.getWriter().write("{\"ifExist\":0}");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传分片
     *
     * @param file
     * @param chunk
     * @param guid
     * @throws IOException
     */
    @PostMapping("save")
    @ResponseBody
    public void upload(@RequestParam MultipartFile file, Integer chunk, String guid) throws IOException {
        String filePath = uploadPath + File.separator + "temp" + File.separator + guid;
        File tempfile = new File(filePath);
        if (!tempfile.exists()) {
            tempfile.mkdirs();
        }
        RandomAccessFile raFile = null;
        BufferedInputStream inputStream = null;
        if (chunk == null) {
            chunk = 0;
        }
        try {
            File dirFile = new File(filePath, String.valueOf(chunk));
            //以读写的方式打开目标文件
            raFile = new RandomAccessFile(dirFile, "rw");
            raFile.seek(raFile.length());
            inputStream = new BufferedInputStream(file.getInputStream());
            byte[] buf = new byte[1024];
            int length = 0;
            while ((length = inputStream.read(buf)) != -1) {
                raFile.write(buf, 0, length);
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        } finally {
            if (inputStream != null) {
                inputStream.close();
            }
            if (raFile != null) {
                raFile.close();
            }
        }
    }

    /**
     * 合并文件
     *
     * @param guid
     * @param fileName
     */
    @RequestMapping(value = "/combine", method = RequestMethod.POST)
    @ResponseBody
    public Map<String, Object> combineBlock(String guid, String fileName, HttpServletResponse response) {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Map<String, Object> map = new HashMap<String, Object>();
        Res res = null;

        //分片文件临时目录
        File tempPath = new File(uploadPath + File.separator + "temp" + File.separator + guid);
        //真实上传路径
        File realPath = new File(uploadPath + File.separator + "real");
        if (!realPath.exists()) {
            realPath.mkdirs();
        }
        /*获取文件类型*/
        String fileTyle = fileName.substring(fileName.lastIndexOf("."), fileName.length());
        /*获取文件名称*/
        fileName = fileName.substring(0, fileName.lastIndexOf("."));
        /*组成一个新的文件名*/
        fileName = fileName + UUID.randomUUID().toString().replaceAll("-", "") + fileTyle;
        File realFile = new File(uploadPath + File.separator + "real" + File.separator + fileName);
        FileOutputStream os = null;// 文件追加写入
        FileChannel fcin = null;
        FileChannel fcout = null;
        try {
            logger.info("合并文件——开始 [ 文件名称：" + fileName + " ，MD5值：" + guid + " ]");
            os = new FileOutputStream(realFile, true);
            fcout = os.getChannel();
            if (tempPath.exists()) {
                //获取临时目录下的所有文件
                File[] tempFiles = tempPath.listFiles();
                //按名称排序
                Arrays.sort(tempFiles, (o1, o2) -> {
                    if (Integer.parseInt(o1.getName()) < Integer.parseInt(o2.getName())) {
                        return -1;
                    }
                    if (Integer.parseInt(o1.getName()) == Integer.parseInt(o2.getName())) {
                        return 0;
                    }
                    return 1;
                });
                //每次读取10MB大小，字节读取
                //byte[] byt = new byte[10 * 1024 * 1024];
                //int len;
                //设置缓冲区为10MB
                ByteBuffer buffer = ByteBuffer.allocate(10 * 1024 * 1024);
                for (int i = 0; i < tempFiles.length; i++) {
                    FileInputStream fis = new FileInputStream(tempFiles[i]);
                    /*while ((len = fis.read(byt)) != -1) {
                        os.write(byt, 0, len);
                    }*/
                    fcin = fis.getChannel();
                    if (fcin.read(buffer) != -1) {
                        buffer.flip();
                        while (buffer.hasRemaining()) {
                            fcout.write(buffer);
                        }
                    }
                    buffer.clear();
                    fis.close();
                    //删除分片
                    tempFiles[i].delete();
                }
                os.close();
                //删除临时目录
                if (tempPath.isDirectory() && tempPath.exists()) {
                    System.gc(); // 回收资源
                    tempPath.delete();
                }


                String filePath = realPath + File.separator + fileName;

                res = resMapper.selectByMD5(guid);
                if (res != null) {
                    logger.info("文件合并1.查询不为空");
                    String s = res.getrPath();
                    logger.info("文件合并1——结束 [ 文件名称：" + fileName + " ，MD5值：" + guid + " ]，文件已存在直接使用已存在的的数据");
                    map.put("fileName", s);
                } else {
                    res = new Res();
                    logger.info("文件合并1.查询空");
                    res.setrMd5(guid);
                    res.setrPath(filePath);
                    int insertResult = resMapper.insert(res);
                    if (insertResult > 0) {
                        logger.info("文件合并1——结束 [ 文件名称：" + fileName + " ，MD5值：" + guid + " ]，成功");
                        map.put("fileName", filePath);
                    } else {
                        logger.info("文件合并1——结束 [ 文件名称：" + fileName + " ，MD5值：" + guid + " ]，失败");
                        map.put("fileName", "文件保存失败请重试");
                    }
                }
            }
        } catch (Exception e) {

            String filePath = realPath + File.separator + fileName;
            /*res = resMapper.selectByMD5(guid);
            if (res != null) {
                logger.info("文件合并.查询不为空");
                String s = res.getrPath();
                logger.info("文件合并——结束 [ 文件名称：" + fileName + " ，MD5值：" + guid + " ]，文件已存在直接使用已存在的的数据");
                map.put("fileName", s);
            } else {
                res = new Res();
                logger.info("文件合并.查询为空");
                res.setrMd5(guid);
                res.setrPath(filePath);
                int insertResult = resMapper.insert(res);
                if (insertResult > 0) {
                    logger.info("文件合并——结束 [ 文件名称：" + fileName + " ，MD5值：" + guid + " ]，成功");
                    map.put("fileName", filePath);
                } else {
                    logger.info("文件合并——结束 [ 文件名称：" + fileName + " ，MD5值：" + guid + " ]，失败");
                    map.put("fileName", "文件保存失败请重试");
                }
            }*/
            map.put("fileName", filePath);
            logger.error("文件合并——失败 " + e.getMessage());
        }

        return map;
    }
}
