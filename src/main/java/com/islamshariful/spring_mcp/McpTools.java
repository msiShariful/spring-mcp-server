package com.islamshariful.spring_mcp;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;


@Component
public class McpTools {

    private static final Logger log = Logger.getLogger(McpTools.class.getName());


    // tool
    @McpTool(name = "latest_videos", description = "This is the tool with latest videos list")
    public String getLatestVideos() {

        return "This is a latest videos list";
    }

    // resources


    // prompts

}
