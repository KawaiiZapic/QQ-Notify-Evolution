package cc.chenhe.qqnotifyevo.core

import org.amshove.kluent.*
import org.junit.Test

class NotificationProcessorTest {

    private fun generateGroupTicker(nickName: String, groupName: String, message: String): String {
        return "$nickName($groupName):$message"
    }

    private fun generateGroupContent(nickName: String, message: String, special: Boolean): String {
        val prefix = if (special) "[有关注的内容]" else ""
        return "$prefix$nickName: $message"
    }

    private fun generateFriendTicker(nickName: String, message: String): String {
        return "$nickName: $message"
    }

    private fun generateFriendTitle(
        nickName: String,
        messageNum: Int,
        special: Boolean = false
    ): String {
        return (if (special) "[特别关心]" else "") + nickName + if (messageNum > 1) " (${messageNum}条新消息)" else ""
    }

    private fun generateQzoneTitle(messageNum: Int = 1): String {
        return "QQ空间动态(共${messageNum}条未读)"
    }

    private fun generateHiddenTicker(messageNum: Int = 1): String {
        return "你收到了${messageNum}条新消息"
    }

    @Test
    fun group_ticker_match() {
        val ticker = generateGroupTicker("Bob", "Family(1)", "Hello~")
        val matcher = NotificationProcessor.groupMsgPattern.matcher(ticker)
        matcher.matches().shouldBeTrue()

        matcher.group(1)!! shouldBeEqualTo "Bob"
        matcher.group(2)!! shouldBeEqualTo "Family(1)"
        matcher.group(3)!! shouldBeEqualTo "Hello~"
    }

    @Test
    fun group_ticker_match_multiLines() {
        val ticker = generateGroupTicker("Bob", "Family(1)", "Hello\nhere\nyep")
        val matcher = NotificationProcessor.groupMsgPattern.matcher(ticker)
        matcher.matches().shouldBeTrue()

        matcher.group(1)!! shouldBeEqualTo "Bob"
        matcher.group(2)!! shouldBeEqualTo "Family(1)"
        matcher.group(3)!! shouldBeEqualTo "Hello\nhere\nyep"
    }

    @Test
    fun group_ticker_mismatch_friend() {
        val ticker = generateFriendTicker("Bob", "Hello~")
        val matcher = NotificationProcessor.groupMsgPattern.matcher(ticker)
        matcher.matches().shouldBeFalse()
    }

    @Test
    fun group_special_content_match() {
        val content = generateGroupContent("(id1)", "Yea", true)
        val matcher = NotificationProcessor.groupMsgContentPattern.matcher(content)

        matcher.matches().shouldBeTrue()
        matcher.group(1).shouldNotBeNullOrEmpty()
    }

    @Test
    fun group_nonSpecial_content_match() {
        val content = generateGroupContent("(id1)", "Yea", false)
        val matcher = NotificationProcessor.groupMsgContentPattern.matcher(content)

        matcher.matches().shouldBeTrue()
        matcher.group(1).shouldBeNull()
    }

    @Test
    fun friend_ticker_match() {
        val ticker = generateFriendTicker("Alice", "hi")
        val matcher = NotificationProcessor.msgPattern.matcher(ticker)
        matcher.matches().shouldBeTrue()

        matcher.group(1)!! shouldBeEqualTo "Alice"
        matcher.group(2)!! shouldBeEqualTo "hi"
    }

    @Test
    fun friend_ticker_match_multiLines() {
        val ticker = generateFriendTicker("Alice", "hi\nok\nthanks")
        val matcher = NotificationProcessor.msgPattern.matcher(ticker)
        matcher.matches().shouldBeTrue()

        matcher.group(1)!! shouldBeEqualTo "Alice"
        matcher.group(2)!! shouldBeEqualTo "hi\nok\nthanks"
    }

    @Test
    fun friend_ticker_mismatch_group() {
        val ticker = generateGroupTicker("Alice", "group", "hi")
        val matcher = NotificationProcessor.msgPattern.matcher(ticker)
        matcher.matches().shouldBeFalse()
    }

    @Test
    fun friend_title_match_single() {
        val title = generateFriendTitle("Bob", 1, false)
        val matcher = NotificationProcessor.msgTitlePattern.matcher(title)
        matcher.matches().shouldBeTrue()

        matcher.group(1).shouldBeNull()
        matcher.group(2).shouldBeNull()
    }

    @Test
    fun friend_special_title_match_single() {
        val title = generateFriendTitle("Bob", 1, true)
        val matcher = NotificationProcessor.msgTitlePattern.matcher(title)
        matcher.matches().shouldBeTrue()

        matcher.group(1).shouldNotBeNull()
        matcher.group(2).shouldBeNull()
    }

    @Test
    fun friend_title_match_multi() {
        val title = generateFriendTitle("Bob", 11, false)
        val matcher = NotificationProcessor.msgTitlePattern.matcher(title)
        matcher.matches().shouldBeTrue()

        matcher.group(1).shouldBeNull()
        matcher.group(2)!!.toInt() shouldBeEqualTo 11
    }

    @Test
    fun friend_special_title_match_multi() {
        val title = generateFriendTitle("Bob", 11, true)
        val matcher = NotificationProcessor.msgTitlePattern.matcher(title)
        matcher.matches().shouldBeTrue()

        matcher.group(1).shouldNotBeNull()
        matcher.group(2)!!.toInt() shouldBeEqualTo 11
    }

    @Test
    fun qzone_title_match() {
        val title = generateQzoneTitle(2)
        val matcher = NotificationProcessor.qzonePattern.matcher(title)
        matcher.matches().shouldBeTrue()
        matcher.group(1)!!.toInt() shouldBeEqualTo 2
    }

    @Test
    fun hidden_message_match() {
        val ticker = generateHiddenTicker()
        val matcher = NotificationProcessor.hideMsgPattern.matcher(ticker)
        matcher.matches().shouldBeTrue()
    }

    @Test
    fun hidden_message_mismatch_friend() {
        val ticker = generateFriendTicker("Bob", "Hello~")
        val matcher = NotificationProcessor.hideMsgPattern.matcher(ticker)
        matcher.matches().shouldBeFalse()
    }

    @Test
    fun hidden_message_mismatch_group() {
        val ticker = generateGroupTicker("Alice", "group", "hi")
        val matcher = NotificationProcessor.hideMsgPattern.matcher(ticker)
        matcher.matches().shouldBeFalse()
    }

    @Test
    fun chat_message_num_match() {
        val title = "Bob (2条新消息)"
        val matcher = NotificationProcessor.msgTitlePattern.matcher(title)
        matcher.matches().shouldBeTrue()
        matcher.group(2)!!.toInt() shouldBeEqualTo 2
    }

    @Test
    fun chat_message_num_mismatch() {
        val title = generateFriendTitle("Bob", 1)
        val matcher = NotificationProcessor.msgTitlePattern.matcher(title)
        matcher.matches().shouldBeTrue()
        matcher.group(2).shouldBeNull()
    }
}