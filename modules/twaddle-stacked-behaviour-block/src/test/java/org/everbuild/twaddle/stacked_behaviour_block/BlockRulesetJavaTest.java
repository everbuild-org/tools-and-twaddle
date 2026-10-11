package org.everbuild.twaddle.stacked_behaviour_block;

import net.kyori.adventure.key.Key;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.Tag;
import java.util.Set;
import org.everbuild.twaddle.stacked_behaviour.ActivityContext;
import org.everbuild.twaddle.stacked_behaviour.ActivityKey;
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset;
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRulesetEditor;
import org.everbuild.twaddle.stacked_behaviour_block.configuration.BlockEntityConfiguration;
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityKt.getPlacementActivity;
import static org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityKt.getDestroyActivity;
import static org.everbuild.twaddle.stacked_behaviour_block.type.TouchActivityKt.getTouchActivity;
import static org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityKt.getTickActivity;

class BlockRulesetJavaTest {
    @Test
    void selectionsCreateMissingBlocksAndPreserveExistingActivitiesFromJava() {
        var original = BlockRuleset.build(editor -> editor.blocks(
            behaviour -> behaviour.activity(ACTIVITY).clear(), Block.STONE));
        var modified = original.modify(editor -> {
            editor.blocks(behaviour -> behaviour.configuration(String.class, "shared"),
                Block.STONE, Block.DIRT, Block.STONE);
            editor.blocks(behaviour -> behaviour.activity(ACTIVITY).clear(),
                Block.DIRT.key(), Block.GOLD_BLOCK.key());
        });
        assertEquals(3, modified.getBlocks().size());
        assertNotNull(modified.get(Block.STONE.key()).activity(ACTIVITY));
        assertNotNull(modified.get(Block.DIRT.key()).activity(ACTIVITY));
        assertNotNull(modified.get(Block.GOLD_BLOCK.key()).activity(ACTIVITY));
        assertEquals("shared", modified.get(Block.STONE.key()).configuration(String.class));
        assertEquals("shared", modified.get(Block.DIRT.key()).configuration(String.class));
        assertNull(original.get(Block.STONE.key()).configuration(String.class));
        assertNull(original.get(Block.DIRT.key()));
    }

    @Test
    void blockHandlerActivitiesCanBeRegisteredAndTickingDisabledFromJava() {
        var ruleset = BlockRuleset.build(editor -> editor.blocks(behaviour -> {
            behaviour.activity(getPlacementActivity()).clear();
            behaviour.activity(getDestroyActivity()).clear();
            behaviour.activity(getTouchActivity()).clear();
            behaviour.activity(getTickActivity()).clear();
        }, Block.STONE));
        var definition = ruleset.get(Block.STONE.key());
        assertNotNull(definition.activity(getPlacementActivity()));
        assertNotNull(definition.activity(getDestroyActivity()));
        assertNotNull(definition.activity(getTouchActivity()));
        assertTrue(RuleBlockHandlerFactory.INSTANCE.create(
            Block.STONE.key(), definition, new LoopingRuleChainApplicatorFactory()).isTickable());
        var removed = ruleset.modify(editor -> editor.blocks(
            behaviour -> behaviour.activity(getTickActivity()).omit(), Block.STONE));
        assertFalse(RuleBlockHandlerFactory.INSTANCE.create(
            Block.STONE.key(), removed.get(Block.STONE.key()), new LoopingRuleChainApplicatorFactory()).isTickable());
    }

    @Test
    void configurationsCanBeSetReadAndRemovedFromJava() {
        var tag = Tag.String("inventory");
        var configuration = new BlockEntityConfiguration((byte) 1, Set.of(tag));
        var original = BlockRuleset.build(editor -> editor.blocks(behaviour -> {
            assertNull(behaviour.configuration(BlockEntityConfiguration.class));
            behaviour.configuration(BlockEntityConfiguration.class, configuration);
            assertSame(configuration, behaviour.configuration(BlockEntityConfiguration.class));
        }, Block.CHEST));
        var definition = original.get(Block.CHEST.key());
        assertSame(configuration, definition.configuration(BlockEntityConfiguration.class));
        var handler = RuleBlockHandlerFactory.INSTANCE.create(
            Block.CHEST.key(), definition, new LoopingRuleChainApplicatorFactory());
        assertEquals((byte) 1, handler.getBlockEntityAction());
        assertEquals(Set.of(tag), handler.getBlockEntityTags());
        assertThrows(UnsupportedOperationException.class, () -> configuration.getTags().clear());
        var removed = original.modify(editor -> editor.blocks(
            behaviour -> behaviour.removeConfiguration(BlockEntityConfiguration.class), Block.CHEST));
        assertNull(removed.get(Block.CHEST.key()).configuration(BlockEntityConfiguration.class));
        assertSame(configuration, definition.configuration(BlockEntityConfiguration.class));
    }

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
            editor.blocks(behaviour -> behaviour.activity(ACTIVITY).clear(), stone);
            editor.blocks(behaviour -> behaviour.activity(ACTIVITY).clear(), Block.DIRT);
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
    void retainOnlyAcceptsKeysBlocksAndAnEmptySelectionFromJava() {
        var original = BlockRuleset.build(editor -> {
            editor.blocks(behaviour -> behaviour.activity(ACTIVITY).clear(), Block.STONE);
            editor.blocks(_ -> {}, Block.DIRT);
        });
        var byBlock = original.modify(editor -> editor.retainOnly(Block.STONE, Block.STONE, Block.GOLD_BLOCK));
        var byKey = original.modify(editor -> editor.retainOnly(Block.STONE.key()));
        assertEquals(byKey.getBlocks(), byBlock.getBlocks());
        assertEquals(1, byBlock.getBlocks().size());
        assertSame(original.get(Block.STONE.key()), byBlock.get(Block.STONE.key()));
        assertEquals(2, original.getBlocks().size());
        assertTrue(original.modify(editor -> editor.retainOnly()).getBlocks().isEmpty());
    }

    @Test
    void concreteEditorCanAlsoBeConstructedAndBuiltFromJava() {
        var editor = new BlockRulesetEditor();
        var first = Key.key("test:first");
        var second = Key.key("test:second");
        editor.blocks(behaviour -> behaviour.activity(ACTIVITY).clear(), first);
        editor.blocks(behaviour -> behaviour.activity(ACTIVITY).clear(), second);
        var snapshot = editor.build();
        editor.blocks(behaviour -> behaviour.activity(ACTIVITY).omit(), first, second);
        assertNotNull(snapshot.get(first).activity(ACTIVITY));
        assertNull(editor.build().get(first).activity(ACTIVITY));
        editor.blocks(behaviour -> {}, first);
        assertNull(editor.build().get(first).activity(ACTIVITY));
    }
}
