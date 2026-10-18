"""Generate version-specific source overlays from the current 1.8.0 code.

Only this directory is generated. Latest 26.x sources are never rewritten.
Official Mojang mapping signatures select API changes, rather than just labels.
"""
import argparse
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LATEST = ROOT.parent / 'src/main'
PACKAGE = Path('com/uncraftbar/easyautocycler')

def replace_block(text, start, replacement):
    pos = text.index(start)
    brace = text.index('{', pos)
    depth = 1
    end = brace + 1
    while depth:
        depth += (text[end] == '{') - (text[end] == '}')
        end += 1
    return text[:pos] + replacement + text[end:]

def generate(mc):
    meta = next(x for x in json.loads((ROOT/'matrix.json').read_text(encoding='utf8')) if x['minecraft'] == mc)
    mappings = (ROOT/'metadata'/f'mappings-{mc}.txt').read_text(encoding='utf8')
    def block(name):
        match = re.search(r'^' + re.escape(name) + r' -> .*?\n(?:(?:[ #\t].*|)\n)*', mappings, re.M)
        return match.group() if match else ''
    modern_ids = bool(block('net.minecraft.resources.Identifier'))
    components = bool(block('net.minecraft.core.component.DataComponents'))
    dynamic_enchants = tuple(map(int, mc.split('.'))) >= (1,21)
    events = bool(block('net.minecraft.client.input.KeyEvent'))
    gui_registry = block('net.minecraft.core.Registry')
    target = ROOT/'generated'/mc
    java = target/'java'
    resources = target/'resources'
    java.mkdir(parents=True, exist_ok=True)
    shutil.copytree(LATEST/'resources', resources, dirs_exist_ok=True)
    sources = {str(p.relative_to(LATEST/'java')).replace('\\','/'):p.read_text(encoding='utf8')
               for p in (LATEST/'java').rglob('*.java')}
    def key(name): return str(PACKAGE/name).replace('\\','/')
    sources.pop(key('mixin/VillagerTradeAccessor.java'))
    sources.pop(key('gui/CustomImageButton.java'))

    item = sources[key('gui/ItemNameResolver.java')]
    for imp in ('com.uncraftbar.easyautocycler.mixin.VillagerTradeAccessor',
                'net.minecraft.world.entity.npc.villager.VillagerProfession',
                'net.minecraft.world.item.trading.TradeSet', 'net.minecraft.world.item.trading.TradeCost',
                'net.minecraft.world.item.trading.VillagerTrade'):
        item = item.replace('import '+imp+';\n', '')
    item = replace_block(item, '    private static Set<Identifier> collectTradeItems(', '''    private static Set<Identifier> collectTradeItems(RegistryAccess access, Identifier professionId,
                                                     ResourceManager resources) {
        Set<Identifier> result = com.uncraftbar.easyautocycler.compat.TradeItemsCompat.collect(access, professionId);
        addVanillaFallback(result, access.lookupOrThrow(Registries.ITEM), professionId, resources);
        return result;
    }''')
    item = replace_block(item, '    private static void addCostItem(', '')
    sources[key('gui/ItemNameResolver.java')] = item

    potion = sources[key('gui/PotionNameResolver.java')]
    potion = potion.replace('import net.minecraft.tags.PotionTags;\n','')
    start = potion.index('        var tradeable =')
    end = potion.index('        ClientLanguage english', start)
    fallback = potion[potion.index('            resources.getResource',start):end]
    fallback = fallback[:fallback.rindex('        }')]
    potion = potion[:start] + fallback + potion[end:]
    if 'java.lang.String name()' not in block('net.minecraft.world.item.alchemy.Potion'):
        potion = potion.replace('potion.name()', 'id.getPath().replaceFirst("^(long_|strong_)", "")')
    if not components:
        potion = potion.replace('effect.getEffect().value()', 'effect.getEffect()')
    sources[key('gui/PotionNameResolver.java')] = potion

    enchant = sources[key('gui/EnchantmentNameResolver.java')]
    if not dynamic_enchants:
        enchant = enchant.replace('import net.minecraft.tags.EnchantmentTags;\n','')
        enchant = enchant.replace('enchantment.description()', 'Component.translatable(enchantment.getDescriptionId())')
        enchant = re.sub(r'boolean doublePrice = registry.get\(id\).*?\.orElse\(false\);',
                         'boolean doublePrice = registry.getValue(id) != null && registry.getValue(id).isTreasureOnly();', enchant, flags=re.S)
    enchant = enchant.replace('Enchantment.MAX_LEVEL','255')
    sources[key('gui/EnchantmentNameResolver.java')] = enchant

    core = sources[key('AutomationManager.java')]
    for imp in ('net.minecraft.core.component.DataComponents','net.minecraft.world.item.enchantment.ItemEnchantments',
                'net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket',
                'net.minecraft.network.protocol.common.custom.CustomPacketPayload'):
        core = core.replace('import '+imp+';\n','')
    core = core.replace('import com.uncraftbar.easyautocycler.compat.MinecraftScreenCompat;',
                        'import com.uncraftbar.easyautocycler.compat.MinecraftScreenCompat;\nimport com.uncraftbar.easyautocycler.compat.StackCompat;\nimport com.uncraftbar.easyautocycler.compat.NativeCycleCompat;')
    core = replace_block(core, '    private static class TradeCyclingHandler', '''    private static class TradeCyclingHandler {
        public boolean canCycle(MerchantMenu menu) { return NativeCycleCompat.canCycle(menu); }
        public boolean sendCyclePacket() { return NativeCycleCompat.cycle(); }
    }''')
    core = core.replace('new TradeCyclingHandler()', 'new TradeCyclingHandler()')
    core = replace_block(core, '    private boolean matchesExactItemEnchantments(', '''    private boolean matchesExactItemEnchantments(ItemStack stack, List<FilterEntry.EnchantmentTarget> targets) {
        Map<Identifier, Integer> found = StackCompat.enchantments(stack);
        if (found.size() != targets.size()) return false;
        return targets.stream().allMatch(t -> found.getOrDefault(t.id(), 0) == t.level());
    }''')
    core = replace_block(core, '    private boolean matchesEnchantmentOnStack(', '''    private boolean matchesEnchantmentOnStack(ItemStack stack, Identifier target, int level, boolean exact) {
        int actual = StackCompat.enchantments(stack).getOrDefault(target, 0);
        return exact ? actual == level : actual >= level;
    }''')
    core = replace_block(core, '    private boolean enchantmentsMatch(', '')
    sources[key('AutomationManager.java')] = core
    model = sources[key('filter/FilterEntry.java')]
    model = model.replace('import net.minecraft.core.component.DataComponents;\n','').replace('import net.minecraft.world.item.alchemy.PotionContents;\n','')
    model = replace_block(model, '    public boolean matchesPotion(', '''    public boolean matchesPotion(ItemStack stack) {
        return potionId == null || (stack.is(Items.TIPPED_ARROW) && potionId.equals(
                com.uncraftbar.easyautocycler.compat.StackCompat.potionId(stack)));
    }''')
    sources[key('filter/FilterEntry.java')] = model

    for template in (ROOT/'templates').rglob('*.java'):
        sources[key(str(template.relative_to(ROOT/'templates')))] = template.read_text(encoding='utf8')
    native_key = key('compat/NativeCycleCompat.java')
    sources[native_key] = sources[native_key].replace('PRESS_BUTTON',
        'button.onPress(new net.minecraft.client.input.KeyEvent(82,0,0))' if events else 'button.onPress()')
    callback = '''(s, event) -> {
            var key=KeyMappingHelper.getBoundKeyOf(Keybindings.toggleAutoTradeKey);
            if(key.getType()==InputConstants.Type.KEYSYM && event.key()==key.getValue()) AutomationManager.INSTANCE.toggle();
            var settings=KeyMappingHelper.getBoundKeyOf(Keybindings.openConfigKey);
            if(settings.getType()==InputConstants.Type.KEYSYM && event.key()==settings.getValue()) client.setScreen(new ConfigScreen(s,ConfigScreen.titleFor(s)));
        }''' if events else '''(s, keyCode, scanCode, modifiers) -> {
            var key=KeyMappingHelper.getBoundKeyOf(Keybindings.toggleAutoTradeKey);
            if(key.getType()==InputConstants.Type.KEYSYM && keyCode==key.getValue()) AutomationManager.INSTANCE.toggle();
            var settings=KeyMappingHelper.getBoundKeyOf(Keybindings.openConfigKey);
            if(settings.getType()==InputConstants.Type.KEYSYM && keyCode==settings.getValue()) client.setScreen(new ConfigScreen(s,ConfigScreen.titleFor(s)));
        }'''
    sources[key('ClientEventHandler.java')] = sources[key('ClientEventHandler.java')].replace('KEY_CALLBACK',callback)
    stack = '''package com.uncraftbar.easyautocycler.compat;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.Map;
import java.util.HashMap;
public final class StackCompat {
    public static Map<Identifier,Integer> enchantments(ItemStack stack) {
        Map<Identifier,Integer> result = new HashMap<>();
        ENCHANTMENTS
        return result;
    }
    public static Identifier potionId(ItemStack stack) {
        POTION
    }
}'''
    if components:
        ench_code = '''var enchantments = stack.getOrDefault(stack.is(Items.ENCHANTED_BOOK)
            ? net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS
            : net.minecraft.core.component.DataComponents.ENCHANTMENTS,
            net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        for (var entry : enchantments.entrySet()) {
            Identifier id = ENCH_KEY;
            if (id != null) result.put(id, entry.getIntValue());
        }'''
        ench_code = ench_code.replace('ENCH_KEY','entry.getKey().unwrapKey().map(k -> k.identifier()).orElse(null)')
        potion_code = '''var contents = stack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
        return contents == null ? null : contents.potion().flatMap(p -> p.unwrapKey()).map(k -> k.identifier()).orElse(null);'''
    else:
        ench_code = '''net.minecraft.world.item.enchantment.EnchantmentHelper.getEnchantments(stack)
                .forEach((enchantment, level) -> result.put(BuiltInRegistries.ENCHANTMENT.getKey(enchantment), level));'''
        potion_code = 'return BuiltInRegistries.POTION.getKey(net.minecraft.world.item.alchemy.PotionUtils.getPotion(stack));'
    sources[key('compat/StackCompat.java')] = stack.replace('ENCHANTMENTS',ench_code).replace('POTION',potion_code)
    sources[key('compat/MinecraftScreenCompat.java')] = '''package com.uncraftbar.easyautocycler.compat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
public final class MinecraftScreenCompat {
    public static Screen getScreen(Minecraft mc) { return mc.screen; }
    public static void setScreen(Minecraft mc, Screen screen) { mc.setScreen(screen); }
}'''

    for path, text in sources.items():
        text = text.replace('net.minecraft.client.gui.GuiGraphicsExtractor','net.minecraft.client.gui.GuiGraphics')
        text = text.replace('GuiGraphicsExtractor','GuiGraphics').replace('extractRenderState','render')
        if not block('net.minecraft.world.entity.npc.villager.VillagerTrades'):
            text = text.replace('net.minecraft.world.entity.npc.villager.VillagerTrades', 'net.minecraft.world.entity.npc.VillagerTrades')
        text = text.replace('graphics.text(', 'graphics.drawString(')
        text = text.replace('net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper',
                            'net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper').replace('KeyMappingHelper','KeyBindingHelper')
        text = text.replace('registerKeyMapping(', 'registerKeyBinding(')
        text = text.replace('mc.player.sendSystemMessage(message)', 'mc.gui.getChat().addMessage(message)')
        if not modern_ids:
            text = text.replace('Identifier','ResourceLocation')
            if 'fromNamespaceAndPath(java.lang.String,java.lang.String)' not in block('net.minecraft.resources.ResourceLocation'):
                text = text.replace('ResourceLocation.fromNamespaceAndPath(', 'new ResourceLocation(')
                text = text.replace('ResourceLocation.withDefaultNamespace(', 'new ResourceLocation(')
                text = text.replace('ResourceLocation.parse(', 'new ResourceLocation(')
            text = text.replace('.identifier()', '.location()')
        if 'lookupOrThrow(net.minecraft.resources.ResourceKey)' not in block('net.minecraft.core.RegistryAccess'):
            text = text.replace('.lookupOrThrow(', '.registryOrThrow(')
        if not re.search(r'java.util.Optional get\(net.minecraft.resources.(?:Identifier|ResourceLocation)\)',gui_registry):
            for variable in ('registry','potions'):
                text = text.replace(variable+'.get(id)',variable+'.getHolder(id)')
        if ' getValue(' not in gui_registry:
            text = re.sub(r'\.getValue\((?=\w)', '.get(', text)
        if not events:
            text = text.replace('import net.minecraft.client.input.KeyEvent;\n','').replace('import net.minecraft.client.input.MouseButtonEvent;\n','')
            text = text.replace('keyPressed(KeyEvent event)', 'keyPressed(int keyCode, int scanCode, int modifiers)')
            text = text.replace('super.keyPressed(event)', 'super.keyPressed(keyCode, scanCode, modifiers)').replace('event.key()', 'keyCode')
            text = text.replace('mouseClicked(MouseButtonEvent event, boolean doubleClick)', 'mouseClicked(double mouseX, double mouseY, int button)')
            text = text.replace('super.mouseClicked(event, doubleClick)', 'super.mouseClicked(mouseX, mouseY, button)')
            text = text.replace('clickSuggestion(MouseButtonEvent event)', 'clickSuggestion(double mouseX, double mouseY, int button)')
            text = text.replace('clickSuggestion(event)', 'clickSuggestion(mouseX, mouseY, button)')
            text = text.replace('event.button()', 'button').replace('event.x()', 'mouseX').replace('event.y()', 'mouseY')
        if 'moveCursorToEnd(boolean)' not in block('net.minecraft.client.gui.components.EditBox'):
            text = text.replace('moveCursorToEnd(false)', 'moveCursorToEnd()')
        if not block('net.minecraft.client.KeyMapping$Category'):
            text = re.sub(r'    public static final KeyMapping.Category CATEGORY =.*?;\n', '', text,flags=re.S)
            text = text.replace('CATEGORY\n', '"key.category.easyautocycler"\n')
        text = text.replace('.getFirst()', '.get(0)')
        dest = java/path
        dest.parent.mkdir(parents=True, exist_ok=True)
        dest.write_text(text,encoding='utf8')

    metadata = json.loads((resources/'fabric.mod.json').read_text(encoding='utf8'))
    metadata['version'] = '1.8.0'
    metadata['depends']['minecraft'] = mc
    metadata['depends']['java'] = '>='+str(meta['java'])
    metadata['depends']['fabricloader'] = '>=0.16.0'
    metadata['depends'].pop('fabric-key-mapping-api-v1',None)
    metadata['depends']['fabric-key-binding-api-v1'] = '*'
    metadata['depends']['trade_cycling'] = '*'
    metadata['suggests']['visibletraders'] = '*'
    (resources/'fabric.mod.json').write_text(json.dumps(metadata,ensure_ascii=False,indent=2),encoding='utf8')
    candidates=json.loads((resources/'assets/easyautocycler/villager_trade_items.json').read_text(encoding='utf8'))
    for profession in ('armorer','toolsmith','weaponsmith'):
        candidates[profession]=sorted(set(candidates[profession]+['minecraft:coal','minecraft:iron_ingot']))
    candidates['cartographer']=sorted(set(candidates['cartographer']+['minecraft:filled_map']))
    for profession in ('cleric','leatherworker'):
        candidates[profession]=sorted(set(candidates[profession]+['minecraft:scute']))
    (resources/'assets/easyautocycler/villager_trade_items.json').write_text(json.dumps(candidates,indent=2),encoding='utf8')
    mixin = json.loads((resources/'easyautocycler.mixins.json').read_text(encoding='utf8'))
    mixin['client'].remove('VillagerTradeAccessor')
    mixin['compatibilityLevel'] = 'JAVA_'+str(meta['java'])
    mixin['refmap'] = 'easyautocycler.refmap.json'
    (resources/'easyautocycler.mixins.json').write_text(json.dumps(mixin,indent=2),encoding='utf8')
    (resources/'pack.mcmeta').unlink(missing_ok=True)
    # The runtime test is generated using the same stable API adapters as production.
    tests = target/'tests'/PACKAGE
    tests.mkdir(parents=True,exist_ok=True)
    test = (ROOT/'templates/BackportChecks.txt').read_text(encoding='utf8')
    test = test.replace('CREATE_ARROW',
        'net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.TIPPED_ARROW, potion)' if components
        else 'net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(Items.TIPPED_ARROW), potion)')
    test = test.replace('POTION_TYPE','net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion>' if components else 'net.minecraft.world.item.alchemy.Potion')
    test = test.replace('OFFER_CREATE', 'new MerchantOffer(new ItemCost(Items.EMERALD, 2), Optional.of(new ItemCost(Items.ARROW, 5)), arrow, 12, 30, 0.05F)' if components
                        else 'new MerchantOffer(new ItemStack(Items.EMERALD, 2), new ItemStack(Items.ARROW, 5), arrow, 12, 30, 0.05F)')
    test = test.replace('ENCHANT_ARROW', '''var enchants = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(
            net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
        enchants.set(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3);
        sword.set(net.minecraft.core.component.DataComponents.ENCHANTMENTS, enchants.toImmutable());''' if components and not dynamic_enchants
        else '' if dynamic_enchants else 'net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(Map.of(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING, 3), sword);')
    test = test.replace('ASSERT_ENCHANT', '' if dynamic_enchants else 'require(StackCompat.enchantments(sword).getOrDefault(id("unbreaking"),0)==3,"enchantment adapter");')
    test = test.replace('Identifier','Identifier' if modern_ids else 'ResourceLocation')
    if 'java.lang.String getName()' not in block('net.minecraft.WorldVersion'):
        test = test.replace('getCurrentVersion().getName()', 'getCurrentVersion().name()')
    if not modern_ids and 'withDefaultNamespace(java.lang.String)' not in block('net.minecraft.resources.ResourceLocation'):
        test = test.replace('ResourceLocation.withDefaultNamespace(', 'new ResourceLocation(')
    test = test.replace('import net.minecraft.world.item.trading.ItemCost;', '' if not components else 'import net.minecraft.world.item.trading.ItemCost;')
    (tests/'BackportChecks.java').write_text(test,encoding='utf8')
    print('Generated', mc, flush=True)

if __name__ == '__main__':
    parser=argparse.ArgumentParser()
    parser.add_argument('versions',nargs='*')
    args=parser.parse_args()
    for version in args.versions or [x['minecraft'] for x in json.loads((ROOT/'matrix.json').read_text(encoding='utf8'))]:
        generate(version)
