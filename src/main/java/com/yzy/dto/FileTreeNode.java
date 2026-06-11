package com.yzy.dto;

import lombok.Data;
import java.util.List;

/**
 * 文件树节点
 */
@Data
public class FileTreeNode {
    /**
     * 文件/文件夹名称
     */
    private String name;

    /**
     * 类型：file 或 directory
     */
    private String type;

    /**
     * 相对路径（仅文件有）
     */
    private String path;

    /**
     * 子节点（仅目录有）
     */
    private List<FileTreeNode> children;
}
