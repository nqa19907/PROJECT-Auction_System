package auction_system.server.network.command;

import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Command mới đọc payload JSON theo tên field, không phụ thuộc thứ tự parts[].
 */
public interface JsonPayloadCommand {

    String execute(JsonNode payload, ClientSession session);
}