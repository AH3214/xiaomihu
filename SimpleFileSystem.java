package work;

import java.util.Scanner;

// �û���
class User {
    String username;
    String password;
    FileDescriptor[] files = new FileDescriptor[10]; // ÿ���û����10���ļ�
    int fileCount = 0; // ��ǰ�ļ�����

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }
}

// �ļ�������
class FileDescriptor {
    String filename;      // �ļ���
    int blockAddress;     // �����ַ�����̿�ţ�
    String protection;    // �����루�� "rw"��"r-"��
    int length;           // �ļ�����
    String owner;         // �ļ�������

    public FileDescriptor(String filename, int blockAddress, String protection, String owner) {
        this.filename = filename;
        this.blockAddress = blockAddress;
        this.protection = protection;
        this.owner = owner;
        this.length = 0;  // ��ʼ����Ϊ0
    }
}

// �ļ�ϵͳ����
public class SimpleFileSystem {
    // ����ģ�⣨100���飬ÿ����512�ֽڣ�
    private static final int BLOCK_SIZE = 512;
    private static final int DISK_SIZE = 100;
    private String[] disk = new String[DISK_SIZE];

    // ���п����λͼ��
    private boolean[] freeBlocks = new boolean[DISK_SIZE];

    // �û���������ʵ�֣�
    private User[] users = new User[10]; // ���10���û�
    private int userCount = 0;
    private String currentUser = null;

    // ���ļ�������ʵ�֣�
    private FileDescriptor[] openFileTable = new FileDescriptor[5]; // ����5���ļ�
    private int openFileCount = 0;

    public SimpleFileSystem() {
        // ��ʼ������
        for (int i = 0; i < DISK_SIZE; i++) {
            disk[i] = null;
        }

        // ��ʼ�����п飨ȫ�����ã�
        for (int i = 0; i < DISK_SIZE; i++) {
            freeBlocks[i] = true;
        }

        // ����Ĭ���û�
        addUser("user1", "pass1");
        addUser("user2", "pass2");
    }

    // ����û�
    private void addUser(String username, String password) {
        users[userCount++] = new User(username, password);
    }

    // �û���¼
    public boolean login(String username, String password) {
        for (int i = 0; i < userCount; i++) {
            User user = users[i];
            if (user.username.equals(username) && user.password.equals(password)) {
                currentUser = username;
                System.out.println("�û� " + username + " ��¼�ɹ�");
                return true;
            }
        }
        System.out.println("��¼ʧ�ܣ��û������������");
        return false;
    }

    // �˳���¼
    public void logout() {
        if (currentUser != null) {
            // �ر����д򿪵��ļ�
            openFileTable = new FileDescriptor[5];
            openFileCount = 0;
            System.out.println("�û� " + currentUser + " ���˳�");
            currentUser = null;
        }
    }

    // ��ȡ��ǰ�û�����
    private User getCurrentUser() {
        for (int i = 0; i < userCount; i++) {
            if (users[i].username.equals(currentUser)) {
                return users[i];
            }
        }
        return null;
    }



    // �ڵ�ǰ�û�Ŀ¼�в����ļ�
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

    // �����ļ�
    public boolean createFile(String filename, String protection) {
        checkLogin();
        User user = getCurrentUser();

        // ����ļ��Ƿ��Ѵ���
        if (findFile(filename) != null) {
            System.out.println("����ʧ�ܣ��ļ��Ѵ���");
            return false;
        }

        // ������п�
        int freeBlock = findFreeBlock();
        if (freeBlock == -1) {
            System.out.println("����ʧ�ܣ����̿ռ䲻��");
            return false;
        }

        // �����ļ�����������ӵ��û�Ŀ¼
        FileDescriptor fd = new FileDescriptor(filename, freeBlock, protection, currentUser);
        user.files[user.fileCount++] = fd;
        disk[freeBlock] = "";  // ��ʼ�����̿�
        freeBlocks[freeBlock] = false;

        System.out.println("�ļ� " + filename + " �����ɹ������ַ: " + freeBlock);
        return true;
    }

    // ɾ���ļ�
    public boolean deleteFile(String filename) {
        checkLogin();
        User user = getCurrentUser();

        // �����ļ�λ��
        int fileIndex = -1;
        for (int i = 0; i < user.fileCount; i++) {
            if (user.files[i].filename.equals(filename)) {
                fileIndex = i;
                break;
            }
        }

        if (fileIndex == -1) {
            System.out.println("ɾ��ʧ�ܣ��ļ�������");
            return false;
        }

        FileDescriptor fd = user.files[fileIndex];

        // �ͷŴ��̿�
        freeBlocks[fd.blockAddress] = true;
        disk[fd.blockAddress] = null;

        // ���û�Ŀ¼���Ƴ����ƶ�����Ԫ�أ�
        for (int i = fileIndex; i < user.fileCount - 1; i++) {
            user.files[i] = user.files[i + 1];
        }
        user.fileCount--;

        // �Ӵ��ļ������Ƴ�
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i] == fd) {
                // �ƶ�����Ԫ��
                for (int j = i; j < openFileCount - 1; j++) {
                    openFileTable[j] = openFileTable[j + 1];
                }
                openFileCount--;
                break;
            }
        }

        System.out.println("�ļ� " + filename + " ��ɾ��");
        return true;
    }

    // ���ļ�
    public boolean openFile(String filename) {
        checkLogin();

        // ����ļ��Ƿ����
        FileDescriptor fd = findFile(filename);
        if (fd == null) {
            System.out.println("��ʧ�ܣ��ļ�������");
            return false;
        }

        // ����Ƿ��Ѵ�
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i] == fd) {
                System.out.println("��ʧ�ܣ��ļ��Ѵ�");
                return false;
            }
        }

        // �����ļ����Ƿ�����
        if (openFileCount >= 5) {
            System.out.println("��ʧ�ܣ����ļ�������");
            return false;
        }

        // ������ļ���
        openFileTable[openFileCount++] = fd;
        System.out.println("�ļ� " + filename + " �Ѵ�");
        return true;
    }

    // �ر��ļ�
    public boolean closeFile(String filename) {
        checkLogin();

        // �ڴ��ļ����в���
        int index = -1;
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i].filename.equals(filename)) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            System.out.println("�ر�ʧ�ܣ��ļ�δ��");
            return false;
        }

        // �Ӵ��ļ����Ƴ����ƶ�����Ԫ�أ�
        for (int i = index; i < openFileCount - 1; i++) {
            openFileTable[i] = openFileTable[i + 1];
        }
        openFileCount--;

        System.out.println("�ļ� " + filename + " �ѹر�");
        return true;
    }

    // ���ļ�
    public String readFile(String filename) {
        checkLogin();

        // ���Ҵ򿪵��ļ�
        FileDescriptor fd = null;
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i].filename.equals(filename)) {
                fd = openFileTable[i];
                break;
            }
        }

        if (fd == null) {
            System.out.println("��ȡʧ�ܣ��ļ�δ��");
            return null;
        }

        // ����Ȩ��
        if (!fd.protection.startsWith("r")) {
            System.out.println("��ȡʧ�ܣ��޶�Ȩ��");
            return null;
        }

        String content = disk[fd.blockAddress];
        System.out.println("��ȡ�ļ� " + filename + " �ɹ�");
        return content;
    }

    // д�ļ�
    public boolean writeFile(String filename, String content) {
        checkLogin();

        // ���Ҵ򿪵��ļ�
        FileDescriptor fd = null;
        for (int i = 0; i < openFileCount; i++) {
            if (openFileTable[i].filename.equals(filename)) {
                fd = openFileTable[i];
                break;
            }
        }

        if (fd == null) {
            System.out.println("д��ʧ�ܣ��ļ�δ��");
            return false;
        }

        // ���дȨ��
        if (!fd.protection.contains("w")) {
            System.out.println("д��ʧ�ܣ���дȨ��");
            return false;
        }

        // ����ļ���С
        if (content.length() > BLOCK_SIZE) {
            System.out.println("д��ʧ�ܣ��ļ�����512�ֽ�����");
            return false;
        }

        // д�����
        disk[fd.blockAddress] = content;
        fd.length = content.length();  // �����ļ�����

        System.out.println("д���ļ� " + filename + " �ɹ�");
        return true;
    }

    // �г�Ŀ¼
    public void listDirectory() {
        checkLogin();
        User user = getCurrentUser();

        if (user.fileCount == 0) {
            System.out.println("Ŀ¼Ϊ��");
            return;
        }

        System.out.println("�ļ���\t�����ַ\t������\t����\t������");
        System.out.println("----------------------------------");
        for (int i = 0; i < user.fileCount; i++) {
            FileDescriptor fd = user.files[i];
            System.out.printf(
                    "%s\t%d\t%s\t%d\t%s\n",
                    fd.filename, fd.blockAddress, fd.protection, fd.length, fd.owner
            );
        }
    }

    // �������������ҿ��п�
    private int findFreeBlock() {
        for (int i = 0; i < DISK_SIZE; i++) {
            if (freeBlocks[i]) {
                return i;
            }
        }
        return -1;  // �޿��п�
    }

    // ����¼״̬
    private void checkLogin() {
        if (currentUser == null) {
            throw new IllegalStateException("���ȵ�¼");
        }
    }


}