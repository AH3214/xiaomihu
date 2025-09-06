package work;

import java.util.Scanner;

// 用户类
class User {
    String username;
    String password;
    FileDescriptor[] files = new FileDescriptor[10]; // 每个用户最多10个文件
    int fileCount = 0; // 当前文件数量

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

// 文件描述符
class FileDescriptor {
    String filename;      // 文件名
    int blockAddress;     // 物理地址（磁盘块号）
    String protection;    // 保护码（如 "rw"、"r-"）
    int length;           // 文件长度
    String owner;         // 文件所有者

    public FileDescriptor(String filename, int blockAddress, String protection, String owner) {
        this.filename = filename;
        this.blockAddress = blockAddress;
        this.protection = protection;
        this.owner = owner;
        this.length = 0;  // 初始长度为0
    }
}

// 文件系统核心
public class SimpleFileSystem {
    // 磁盘模拟（100个块，每个块512字节）
    private static final int BLOCK_SIZE = 512;
    private static final int DISK_SIZE = 100;
    private String[] disk = new String[DISK_SIZE];

    // 空闲块管理（位图）
    private boolean[] freeBlocks = new boolean[DISK_SIZE];

    // 用户管理（数组实现）
    private User[] users = new User[10]; // 最多10个用户
    private int userCount = 0;
    private String currentUser = null;

    // 打开文件表（数组实现）
    private FileDescriptor[] openFileTable = new FileDescriptor[5]; // 最多打开5个文件
    private int openFileCount = 0;

    public SimpleFileSystem() {
        // 初始化磁盘
        for (int i = 0; i < DISK_SIZE; i++) {
            disk[i] = null;
        }

        // 初始化空闲块（全部可用）
        for (int i = 0; i < DISK_SIZE; i++) {
            freeBlocks[i] = true;
        }

        // 创建默认用户
        addUser("user1", "pass1");
        addUser("user2", "pass2");
    }

    // 添加用户
    private void addUser(String username, String password) {
        users[userCount++] = new User(username, password);
    }

    // 用户登录
    public boolean login(String username, String password) {
        for (int i = 0; i < userCount; i++) {
            User user = users[i];
            if (user.username.equals(username) && user.password.equals(password)) {
                currentUser = username;
                System.out.println("用户 " + username + " 登录成功");
                return true;
            }
        }
        System.out.println("登录失败：用户名或密码错误");
        return false;
    }

    // 退出登录
    public void logout() {
        if (currentUser != null) {
            // 关闭所有打开的文件
            openFileTable = new FileDescriptor[5];
            openFileCount = 0;
            System.out.println("用户 " + currentUser + " 已退出");
            currentUser = null;
        }
    }

    // 获取当前用户对象
    private User getCurrentUser() {
        for (int i = 0; i < userCount; i++) {
            if (users[i].username.equals(currentUser)) {
                return users[i];
            }
        }
        return null;
    }



    // 在当前用户目录中查找文件
    private FileDescriptor findFile(String filename) {
        User user = getCurrentUser();
        if (user == null)
            return null;

        for (int i = 0; i < user.fileCount; i++) {
            if (user.files[i].filename.equals(filename)) {
                return user.files[i];
            }
        }
        return null;
    }

    // 创建文件
    public boolean createFile(String filename, String protection) {
        checkLogin();
        User user = getCurrentUser();

        // 检查文件是否已存在
        if (findFile(filename) != null) {
            System.out.println("创建失败：文件已存在");
            return false;
        }

        // 分配空闲块
        int freeBlock = findFreeBlock();
        if (freeBlock == -1) {
            System.out.println("创建失败：磁盘空间不足");
            return false;
        }

        // 创建文件描述符并添加到用户目录
        FileDescriptor fd = new FileDescriptor(filename, freeBlock, protection, currentUser);
        user.files[user.fileCount++] = fd;
        disk[freeBlock] = "";  // 初始化磁盘块
        freeBlocks[freeBlock] = false;

        System.out.println("文件 " + filename + " 创建成功，块地址: " + freeBlock);
        return true;
    }

    // 删除文件
    public boolean deleteFile(String filename) {
        checkLogin();
        User user = getCurrentUser();

        // 查找文件位置
        int fileIndex = -1;
        for (int i = 0; i < user.fileCount; i++) {
            if (user.files[i].filename.equals(filename)) {
                fileIndex = i;
                break;
            }
        }

        if (fileIndex == -1) {
            System.out.println("删除失败：文件不存在");
            return false;
        }

        FileDescriptor fd = user.files[fileIndex];

        // 释放磁盘块
        freeBlocks[fd.blockAddress] = true;
        disk[fd.blockAddress] = null;

        // 从用户目录中移除（移动后续元素）
        for (int i = fileIndex; i < user.fileCount - 1; i++) {
            user.files[i] = user.files[i + 1];
        }
        user.fileCount--;

        // 从打开文件表中移除
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i] == fd) {
                // 移动后续元素
                for (int j = i; j < openFileCount - 1; j++) {
                    openFileTable[j] = openFileTable[j + 1];
                }
                openFileCount--;
                break;
            }
        }

        System.out.println("文件 " + filename + " 已删除");
        return true;
    }

    // 打开文件
    public boolean openFile(String filename) {
        checkLogin();

        // 检查文件是否存在
        FileDescriptor fd = findFile(filename);
        if (fd == null) {
            System.out.println("打开失败：文件不存在");
            return false;
        }

        // 检查是否已打开
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i] == fd) {
                System.out.println("打开失败：文件已打开");
                return false;
            }
        }

        // 检查打开文件表是否已满
        if (openFileCount >= 5) {
            System.out.println("打开失败：打开文件表已满");
            return false;
        }

        // 加入打开文件表
        openFileTable[openFileCount++] = fd;
        System.out.println("文件 " + filename + " 已打开");
        return true;
    }

    // 关闭文件
    public boolean closeFile(String filename) {
        checkLogin();

        // 在打开文件表中查找
        int index = -1;
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i].filename.equals(filename)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            System.out.println("关闭失败：文件未打开");
            return false;
        }

        // 从打开文件表移除（移动后续元素）
        for (int i = index; i < openFileCount - 1; i++) {
            openFileTable[i] = openFileTable[i + 1];
        }
        openFileCount--;

        System.out.println("文件 " + filename + " 已关闭");
        return true;
    }

    // 读文件
    public String readFile(String filename) {
        checkLogin();

        // 查找打开的文件
        FileDescriptor fd = null;
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i].filename.equals(filename)) {
                fd = openFileTable[i];
                break;
            }
        }

        if (fd == null) {
            System.out.println("读取失败：文件未打开");
            return null;
        }

        // 检查读权限
        if (!fd.protection.startsWith("r")) {
            System.out.println("读取失败：无读权限");
            return null;
        }

        String content = disk[fd.blockAddress];
        System.out.println("读取文件 " + filename + " 成功");
        return content;
    }

    // 写文件
    public boolean writeFile(String filename, String content) {
        checkLogin();

        // 查找打开的文件
        FileDescriptor fd = null;
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i].filename.equals(filename)) {
                fd = openFileTable[i];
                break;
            }
        }

        if (fd == null) {
            System.out.println("写入失败：文件未打开");
            return false;
        }

        // 检查写权限
        if (!fd.protection.contains("w")) {
            System.out.println("写入失败：无写权限");
            return false;
        }

        // 检查文件大小
        if (content.length() > BLOCK_SIZE) {
            System.out.println("写入失败：文件超过512字节限制");
            return false;
        }

        // 写入磁盘
        disk[fd.blockAddress] = content;
        fd.length = content.length();  // 更新文件长度

        System.out.println("写入文件 " + filename + " 成功");
        return true;
    }

    // 列出目录
    public void listDirectory() {
        checkLogin();
        User user = getCurrentUser();

        if (user.fileCount == 0) {
            System.out.println("目录为空");
            return;
        }

        System.out.println("文件名\t物理地址\t保护码\t长度\t所有者");
        System.out.println("----------------------------------");
        for (int i = 0; i < user.fileCount; i++) {
            FileDescriptor fd = user.files[i];
            System.out.printf(
                    "%s\t%d\t%s\t%d\t%s\n",
                    fd.filename, fd.blockAddress, fd.protection, fd.length, fd.owner
            );
        }
    }

    // 辅助方法：查找空闲块
    private int findFreeBlock() {
        for (int i = 0; i < DISK_SIZE; i++) {
            if (freeBlocks[i]) {
                return i;
            }
        }
        return -1;  // 无空闲块
    }

    // 检查登录状态
    private void checkLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("请先登录");
        }
    }


}