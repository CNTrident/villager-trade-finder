package com.uncraftbar.easyautocycler.gui;

import com.uncraftbar.easyautocycler.AutomationManager;
import com.uncraftbar.easyautocycler.filter.FilterEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Paged target rows avoid the incompatible list-entry layouts in older releases. */
public final class ConfigScreen extends Screen {
    private final Screen parent;
    private final ResourceLocation professionId;
    private final boolean librarian;
    private final boolean enchantable;
    private final List<FilterEntry> filters;
    private boolean matchAny;
    private FilterEntry.Mode nextBulk = FilterEntry.Mode.ON;
    private EnchantmentNameResolver enchants;
    private ItemNameResolver items;
    private PotionNameResolver potions;
    private int page;
    private int perPage = 3;

    public ConfigScreen(@Nullable Screen parent, Component title) {
        super(title);
        this.parent = parent;
        ResourceLocation found = AutomationManager.INSTANCE.getVillagerProfessionId(parent);
        this.professionId = found == null ? new ResourceLocation("librarian") : found;
        this.librarian = professionId.getPath().equals("librarian");
        this.enchantable = Set.of("fletcher", "armorer", "toolsmith", "weaponsmith", "fisherman").contains(professionId.getPath());
        this.filters = new ArrayList<>(AutomationManager.INSTANCE.getFilterEntries(professionId));
        this.matchAny = AutomationManager.INSTANCE.isMatchAny(professionId);
    }
    public static Component titleFor(Screen screen) {
        return AutomationManager.INSTANCE.isLibrarianTradeScreen(screen)
                ? Component.translatable("gui.easyautocycler.config.title")
                : Component.translatable("gui.easyautocycler.config.title.profession",screen.getTitle());
    }
    @Override protected void init() {
        clearWidgets();
        if (minecraft == null || minecraft.level == null) { onClose(); return; }
        var access = minecraft.level.registryAccess();
        var resources = minecraft.getResourceManager();
        if (librarian || enchantable) enchants = new EnchantmentNameResolver(access.registryOrThrow(Registries.ENCHANTMENT),resources);
        if (!librarian) items = new ItemNameResolver(access, professionId, resources);
        if (professionId.getPath().equals("fletcher")) potions = new PotionNameResolver(access,resources);
        int w = Math.min(540,width - 20), x = (width-w)/2;
        int addWidth = Math.min(116,(w-12)/3);
        button(Component.translatable("gui.easyautocycler.filters.add_compact"),x,42,addWidth,b -> edit(null));
        var mode = button(Component.literal(matchAny?"OR":"AND"),x+addWidth+6,42,64,b->{ matchAny=!matchAny; init(); });
        mode.setTooltip(Tooltip.create(Component.translatable(matchAny?"gui.easyautocycler.filters.match_any.tooltip":"gui.easyautocycler.filters.match_all.tooltip")));
        button(Component.translatable("gui.easyautocycler.filters.all_"+nextBulk.name().toLowerCase(java.util.Locale.ROOT)),x+addWidth+76,42,w-addWidth-76,b->{
            filters.forEach(f -> f.setMode(nextBulk));
            nextBulk=FilterEntry.Mode.values()[(nextBulk.ordinal()+1)%3]; init();
        });
        perPage=Math.max(1,(height-133)/34);
        int pages=Math.max(1,(filters.size()+perPage-1)/perPage);
        page=Math.min(page,pages-1);
        for(int i=page*perPage;i<Math.min(filters.size(),(page+1)*perPage);i++) {
            var filter=filters.get(i); int y=71+(i-page*perPage)*34;
            var enabled=button(Component.literal(filter.getMode().name()),x,y,42,b->{
                filter.setMode(FilterEntry.Mode.values()[(filter.getMode().ordinal()+1)%3]);init();
            });
            enabled.setTooltip(Tooltip.create(Component.translatable("gui.easyautocycler.filters.mode."+filter.getMode().name().toLowerCase(java.util.Locale.ROOT)+".tooltip")));
            var scope=button(scopeLabel(filter),x+48,y,76,b->{
                filter.setTradeScope(FilterEntry.TradeScope.values()[(filter.getTradeScope().ordinal()+1)%3]);init();
            });
            scope.setTooltip(Tooltip.create(Component.translatable("gui.easyautocycler.filters.scope."+scopeName(filter)+".tooltip")));
            Component display = filter.getDisplayName(filter.getEnchantmentId()==null||enchants==null?null:enchants.displayName(filter.getEnchantmentId()),
                    filter.getItemId()==null||items==null?null:items.displayName(filter.getItemId()),
                    enchants==null?List.of():filter.getItemEnchantments().stream().map(t->enchants.displayName(t.id())).toList(),
                    filter.getPotionId()==null||potions==null?null:potions.displayName(filter.getPotionId()));
            var edit=button(display,x+130,y,w-156,b->edit(filter));
            edit.setTooltip(Tooltip.create(display));
            button(Component.literal("X"),x+w-20,y,20,b->{filters.remove(filter);init();});
        }
        int footer=height-54;
        button(Component.literal("<"),x,footer,40,b->{page=(page+pages-1)%pages;init();});
        button(Component.literal((page+1)+"/"+pages),x+46,footer,64,b->{});
        button(Component.literal(">"),x+116,footer,40,b->{page=(page+1)%pages;init();});
        int third=(w-12)/3;
        button(Component.translatable("gui.easyautocycler.config.save"),x,height-28,third,b->{
            AutomationManager.INSTANCE.setMatchAny(professionId,matchAny);
            AutomationManager.INSTANCE.setFilterEntries(professionId,filters);onClose();
        });
        button(Component.translatable("gui.easyautocycler.config.clear_all"),x+third+6,height-28,third,b->{filters.clear();init();});
        button(Component.translatable("gui.cancel"),x+2*(third+6),height-28,third,b->onClose());
    }
    private static String scopeName(FilterEntry f) {
        return switch(f.getTradeScope()){case INITIAL->"initial";case LATER->"later";case ALL->"all";};
    }
    private static Component scopeLabel(FilterEntry f) {
        return Component.translatable("gui.easyautocycler.filters.scope."+scopeName(f));
    }
    private Button button(Component label,int x,int y,int w,Button.OnPress action) {
        return addRenderableWidget(Button.builder(label,action).bounds(x,y,w,20).build());
    }
    private void edit(FilterEntry entry) {
        FilterEntry working=entry==null?new FilterEntry():new FilterEntry(entry);
        minecraft.setScreen(new FilterEditorScreen(this,working,librarian,enchantable,enchants,items,potions,
                librarian?enchants.suggestions():items.suggestions(), enchants==null?List.of():enchants.suggestions(),
                ignored->{if(entry==null)filters.add(working);else filters.set(filters.indexOf(entry),working);}));
    }
    @Override public void render(GuiGraphics graphics,int mouseX,int mouseY,float delta) {
        graphics.fill(0,0,width,height,0xE0181B22);
        super.render(graphics,mouseX,mouseY,delta);
        int x=(width-Math.min(540,width-20))/2;
        graphics.drawString(font,title,x,12,0xFFF4F6F8,false);
        graphics.drawString(font,Component.translatable("gui.easyautocycler.config.summary",filters.size()),x,27,0xFFAAB2BF,false);
    }
    @Override public void onClose(){minecraft.setScreen(parent);}
    @Override public boolean isPauseScreen(){return false;}
}
