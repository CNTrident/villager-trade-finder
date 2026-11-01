"""Propagate only the entity-context fix to maintained overlays, preserving their other code."""
from pathlib import Path
import json
import re

ROOT = Path(__file__).resolve().parents[1]
VERSION = next(line.split('=', 1)[1] for line in (ROOT / 'gradle.properties').read_text().splitlines()
               if line.startswith('mod_version='))
PACKAGE = Path('com/uncraftbar/easyautocycler')
tracker = (ROOT / 'src/main/java' / PACKAGE / 'VillagerTradeContext.java').read_text(encoding='utf-8')
interaction = (ROOT / 'src/main/java' / PACKAGE / 'mixin/VillagerInteractionMixin.java').read_text(encoding='utf-8')
old_interaction = interaction.replace('Entity entity, EntityHitResult hit,\n                                               InteractionHand hand,',
                                      'Entity entity, InteractionHand hand,')
old_interaction = old_interaction.rstrip()[:-1] + '''
    @Inject(method = "interactAt", at = @At("HEAD"))
    private void easyautocycler$recordVillagerAt(Player player, Entity entity, EntityHitResult hit,
                                               InteractionHand hand, CallbackInfoReturnable<InteractionResult> ci) {
        VillagerTradeContext.recordInteraction(player, entity);
    }
}
'''

for entry in json.loads((ROOT / 'backports/matrix.json').read_text()):
    mc = entry['minecraft']
    version = tuple(map(int, mc.split('.')))
    base = ROOT / 'backports/versions' / mc
    java = base / 'java' / PACKAGE
    text = tracker
    if version < (1, 21, 11):
        text = text.replace('net.minecraft.world.entity.npc.villager.Villager', 'net.minecraft.world.entity.npc.Villager')
    if version < (1, 21, 5):
        text = text.replace('boundVillager.getVillagerData().profession().unwrapKey()\n                .map(key -> key.identifier()).orElse(null)',
                            'net.minecraft.core.registries.BuiltInRegistries.VILLAGER_PROFESSION.getKey(\n                boundVillager.getVillagerData().getProfession())')
    if version < (1, 21, 11):
        text = text.replace('Identifier', 'ResourceLocation').replace('.identifier()', '.location()')
    (java / 'VillagerTradeContext.java').write_text(text, encoding='utf-8')
    (java / 'mixin/VillagerInteractionMixin.java').write_text(old_interaction, encoding='utf-8')
    manager = java / 'AutomationManager.java'
    text = manager.read_text(encoding='utf-8').replace('import net.minecraft.network.chat.contents.TranslatableContents;\n', '')
    text, count = re.subn(r'(public (?:Identifier|ResourceLocation) getVillagerProfessionId\(Screen screen\) \{).*?\n    }',
                         r'\1\n        if (!(screen instanceof MerchantScreen merchantScreen)) return null;\n        return VillagerTradeContext.professionId(merchantScreen.getMenu());\n    }', text, count=1, flags=re.S)
    assert count == 1, mc
    manager.write_text(text, encoding='utf-8')
    handler = java / 'ClientEventHandler.java'
    text = handler.read_text(encoding='utf-8')
    if 'VillagerTradeContext.onScreenBeforeInit(screen);' not in text:
        text, count = re.subn(r'(public static void onScreenBeforeInit\([^\n]+\) \{\n)',
                             r'\1        VillagerTradeContext.onScreenBeforeInit(screen);\n', text, count=1)
        assert count == 1, mc
    handler.write_text(text, encoding='utf-8')
    tick = java / 'mixin/MinecraftTickMixin.java'
    text = tick.read_text(encoding='utf-8')
    if 'VillagerTradeContext.clientTick();' not in text:
        text = text.replace('Minecraft client = (Minecraft) (Object) this;',
                            'Minecraft client = (Minecraft) (Object) this;\n        com.uncraftbar.easyautocycler.VillagerTradeContext.clientTick();')
    tick.write_text(text, encoding='utf-8')
    mixin = base / 'resources/easyautocycler.mixins.json'
    data = json.loads(mixin.read_text(encoding='utf-8'))
    if 'VillagerInteractionMixin' not in data['client']:
        data['client'].append('VillagerInteractionMixin')
    mixin.write_text(json.dumps(data, indent=2) + '\n', encoding='utf-8')
    metadata = base / 'resources/fabric.mod.json'
    data = json.loads(metadata.read_text(encoding='utf-8'))
    data['version'] = VERSION
    metadata.write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print('Updated entity context:', mc)
