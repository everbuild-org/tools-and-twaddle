package org.everbuild.twaddle.stacked_behaviour_block;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.TagHandler;
import org.everbuild.twaddle.stacked_behaviour.ActivityContext;
import org.everbuild.twaddle.stacked_behaviour.ActivityKey;
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset;
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BlockRulesetJavaTest {
    private static final ActivityKey<BlockRuleset, Context> ACTIVITY = new ActivityKey<>("test:java");

    private static final class Context implements ActivityContext {
        private final TagHandler tags = TagHandler.newHandler();

        @Override
        public @NonNull TagHandler tagHandler() {
            return tags;
        }
    }

    @Test
    void javaCallbacksBuildModifyAndRemoveBlocksThroughInstanceMethods() {
        var stone = Block.STONE.key();
        var original = BlockRuleset.build(editor -> {
            editor.addBlock(stone, behaviour -> behaviour.activity(ACTIVITY).clear());
            editor.addBlock(Block.DIRT, behaviour -> behaviour.activity(ACTIVITY).clear());
        });

        BlockRuleset modified = original.modify(editor -> {
            editor.blocks(behaviour -> behaviour.activity(ACTIVITY).omit(), stone);
            editor.blocks(behaviour -> behaviour.activity(ACTIVITY).omit(), Block.DIRT);
        });
        assertNotNull(original.get(stone).activity(ACTIVITY));
        assertNotNull(original.get(Block.DIRT.key()).activity(ACTIVITY));
        assertNull(modified.get(stone).activity(ACTIVITY));
        assertNull(modified.get(Block.DIRT.key()).activity(ACTIVITY));

        BlockRuleset removed = modified.modify(editor -> {
            editor.removeBlocks(stone);
            editor.removeBlocks(Block.DIRT);
        });
        assertTrue(removed.getBlocks().isEmpty());
        assertEquals(2, modified.getBlocks().size());
        assertEquals(2, BlockRuleset.combine(original, removed).getBlocks().size());
    }

    @Test
    void concreteEditorCanAlsoBeConstructedAndBuiltFromJava() {
        var editor = new BlockRulesetEditor();
        var first = Key.key("test:first");
        var second = Key.key("test:second");
        editor.addBlock(first, behaviour -> behaviour.activity(ACTIVITY).clear());
        editor.addBlock(second, behaviour -> behaviour.activity(ACTIVITY).clear());
        var snapshot = editor.build();
        editor.blocks(behaviour -> behaviour.activity(ACTIVITY).omit(), first, second);
        assertNotNull(snapshot.get(first).activity(ACTIVITY));
        assertNull(editor.build().get(first).activity(ACTIVITY));
        assertThrows(IllegalArgumentException.class, () -> editor.addBlock(first, behaviour -> {}));
    }
}
