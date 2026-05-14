package auction_system.server.patterns.command;

import auction_system.server.session.ClientSession;

/**
 * Interface chung cho tất cả các lệnh mà client có thể gửi lên server.
 */
public interface Command {
    String execute(String[] parts, ClientSession session);
}