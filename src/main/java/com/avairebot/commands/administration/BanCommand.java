package com.avairebot.commands.administration;

import com.avairebot.AvaIre;
import com.avairebot.contracts.commands.Command;
import com.avairebot.modules.BanModule;
import net.dv8tion.jda.core.entities.Message;

import java.util.Collections;
import java.util.List;

public class BanCommand extends Command {

    public BanCommand(AvaIre avaire) {
        super(avaire, false);
    }

    @Override
    public String getName() {
        return "Ban Command";
    }

    @Override
    public String getDescription() {
        return "Bans the mentioned user from the server with the provided reason, all messages the user has sent in the last 7 days will also be deleted in the process, this action will be reported to any channel that has modloging enabled.";
    }

    @Override
    public List<String> getUsageInstructions() {
        return Collections.singletonList("`:command <user> [reason]` - Bans the mentioned user with the given reason.");
    }

    @Override
    public List<String> getExampleUsage() {
        return Collections.singletonList("`:command @Senither Spam and acting like a twat`");
    }

    @Override
    public List<String> getTriggers() {
        return Collections.singletonList("ban");
    }

    @Override
    public List<String> getMiddleware() {
        return Collections.singletonList("require:all,general.ban_members");
    }

    @Override
    public boolean onCommand(Message message, String[] args) {
        return BanModule.ban(this, message, args, false);
    }
}