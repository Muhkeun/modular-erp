import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, useEffect, useMemo } from "react";
import { useTranslation } from "react-i18next";
import { Save, ArrowLeft, Trash2, Languages, Package, Ruler, ShieldCheck, Plus } from "lucide-react";
import PageHeader from "../../../shared/components/PageHeader";
import SearchSelect, { type SearchOption } from "../../../shared/components/SearchSelect";
import { ITEM_GROUP_OPTIONS, LOCALE_OPTIONS, UNIT_OPTIONS } from "../../../shared/data/lookups";
import api from "../../../shared/api/client";

interface ItemForm {
  code: string;
  itemType: string;
  itemGroup: string;
  unitOfMeasure: string;
  specification: string;
  weight: string;
  volume: string;
  makerName: string;
  makerItemNo: string;
  qualityInspectionRequired: boolean;
  phantomBom: boolean;
  translations: { locale: string; name: string; description: string }[];
}

const defaultForm: ItemForm = {
  code: "",
  itemType: "MATERIAL",
  itemGroup: "",
  unitOfMeasure: "EA",
  specification: "",
  weight: "",
  volume: "",
  makerName: "",
  makerItemNo: "",
  qualityInspectionRequired: false,
  phantomBom: false,
  translations: [{ locale: "ko", name: "", description: "" }],
};

const ITEM_TYPES = ["MATERIAL", "PRODUCT", "SEMI_PRODUCT", "SERVICE", "ASSET"] as const;

const ITEM_TYPE_DESCRIPTIONS: Record<(typeof ITEM_TYPES)[number], string> = {
  MATERIAL: "원자재, 부자재, 구매성 자재",
  PRODUCT: "판매 또는 출하 대상 완제품",
  SEMI_PRODUCT: "공정 중간 산출물 및 반제품",
  SERVICE: "외주, 서비스, 용역성 항목",
  ASSET: "설비, 금형, 비품 등 자산성 품목",
};

export default function ItemFormPage() {
  const { id } = useParams();
  const isEdit = !!id;
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { t } = useTranslation();
  const [form, setForm] = useState<ItemForm>(defaultForm);

  const { data } = useQuery({
    queryKey: ["item", id],
    queryFn: async () => {
      const res = await api.get(`/api/v1/master-data/items/${id}`);
      return res.data.data;
    },
    enabled: isEdit,
  });

  useEffect(() => {
    if (data) {
      setForm({
        code: data.code,
        itemType: data.itemType,
        itemGroup: data.itemGroup || "",
        unitOfMeasure: data.unitOfMeasure,
        specification: data.specification || "",
        weight: data.weight?.toString() || "",
        volume: data.volume?.toString() || "",
        makerName: data.makerName || "",
        makerItemNo: data.makerItemNo || "",
        qualityInspectionRequired: data.qualityInspectionRequired,
        phantomBom: data.phantomBom,
        translations: data.translations?.length
          ? data.translations
          : [{ locale: "ko", name: "", description: "" }],
      });
    }
  }, [data]);

  const saveMutation = useMutation({
    mutationFn: async () => {
      const body = {
        ...form,
        weight: form.weight ? parseFloat(form.weight) : null,
        volume: form.volume ? parseFloat(form.volume) : null,
      };
      if (isEdit) {
        return api.put(`/api/v1/master-data/items/${id}`, body);
      }
      return api.post("/api/v1/master-data/items", body);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["items"] });
      navigate("/master-data/items");
    },
  });

  const itemTypeOptions = useMemo<SearchOption[]>(
    () =>
      ITEM_TYPES.map((itemType) => ({
        value: itemType,
        label: t(`item.types.${itemType}`),
        description: ITEM_TYPE_DESCRIPTIONS[itemType],
        meta: "Type",
      })),
    [t]
  );

  const set = (field: keyof ItemForm, value: unknown) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const setTranslation = (index: number, field: string, value: string) => {
    const updated = [...form.translations];
    updated[index] = { ...updated[index], [field]: value };
    setForm((prev) => ({ ...prev, translations: updated }));
  };

  const removeTranslation = (index: number) => {
    setForm((prev) => ({
      ...prev,
      translations: prev.translations.filter((_, translationIndex) => translationIndex !== index),
    }));
  };

  return (
    <div className="space-y-6">
      <PageHeader
        title={isEdit ? `${t("item.editItem")} · ${form.code}` : t("item.newItem")}
        description="품목 유형과 기준값은 검색형 lookup으로 선택하고, 운영 속성과 다국어 정보를 카드형 편집 패널로 정리했습니다."
        breadcrumbs={[
          { label: t("nav.masterData"), path: "/master-data" },
          { label: t("nav.items"), path: "/master-data/items" },
          { label: isEdit ? form.code : t("common.new") },
        ]}
        actions={
          <>
            <button className="btn-secondary" onClick={() => navigate("/master-data/items")}>
              <ArrowLeft size={16} /> {t("common.back")}
            </button>
            {isEdit && (
              <button className="btn-danger">
                <Trash2 size={16} /> {t("common.delete")}
              </button>
            )}
            <button className="btn-primary" onClick={() => saveMutation.mutate()} disabled={saveMutation.isPending}>
              <Save size={16} /> {saveMutation.isPending ? t("common.saving") : t("common.save")}
            </button>
          </>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1.75fr)_360px]">
        <div className="space-y-6">
          <section className="workspace-hero">
            <div className="flex flex-col gap-6 lg:flex-row lg:items-end lg:justify-between">
              <div>
                <p className="section-kicker">Master Data Workspace</p>
                <h2 className="section-title">품목 정의를 검색형 폼으로 정리</h2>
                <p className="mt-3 max-w-2xl text-sm text-slate-600">
                  품목유형, 그룹, 단위, 언어는 검색 결과에서 선택하고 나머지 속성은 카드형 패널에서 바로 관리하도록
                  재구성했습니다.
                </p>
              </div>
              <div className="grid w-full gap-3 sm:grid-cols-3 lg:w-[420px]">
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Type</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">
                    {itemTypeOptions.find((option) => option.value === form.itemType)?.label ?? "-"}
                  </div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.18em] text-slate-400">UoM</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{form.unitOfMeasure || "-"}</div>
                </div>
                <div className="stat-tile">
                  <div className="text-xs uppercase tracking-[0.18em] text-slate-400">Locales</div>
                  <div className="mt-2 text-sm font-semibold text-slate-900">{form.translations.length}</div>
                </div>
              </div>
            </div>
          </section>

          <section className="section-card">
            <p className="section-kicker">Item Identity</p>
            <h3 className="section-title">{t("item.basicInfo")}</h3>
            <div className="mt-6 grid grid-cols-1 gap-5 md:grid-cols-2">
              <div className="space-y-2">
                <label className="field-label">{t("item.code")} *</label>
                <input
                  className="input font-mono"
                  value={form.code}
                  disabled={isEdit}
                  onChange={(e) => set("code", e.target.value)}
                  placeholder="ITEM-001"
                />
              </div>
              <SearchSelect
                label={`${t("item.type")} *`}
                value={form.itemType}
                options={itemTypeOptions}
                placeholder="품목유형 검색"
                searchTitle="품목유형 선택"
                searchDescription="운영 목적과 재고/원가 처리 기준에 맞는 유형을 선택합니다."
                onSelect={(option) => set("itemType", option.value)}
              />
              <SearchSelect
                label={t("item.group")}
                value={form.itemGroup}
                options={ITEM_GROUP_OPTIONS}
                placeholder="품목그룹 검색"
                searchTitle="품목그룹 선택"
                searchDescription="분류 체계에 맞는 그룹을 검색해 선택합니다."
                onSelect={(option) => set("itemGroup", option.value)}
                onClear={() => set("itemGroup", "")}
              />
              <SearchSelect
                label={t("item.uom")}
                value={form.unitOfMeasure}
                options={UNIT_OPTIONS}
                placeholder="단위 검색"
                searchTitle="단위 선택"
                onSelect={(option) => set("unitOfMeasure", option.value)}
              />
              <div className="space-y-2 md:col-span-2">
                <label className="field-label">{t("item.spec")}</label>
                <textarea
                  className="input min-h-[116px] resize-none"
                  value={form.specification}
                  onChange={(e) => set("specification", e.target.value)}
                  placeholder="재질, 규격, 색상, 치수 등 검색 결과에 함께 노출할 정보를 입력"
                />
              </div>
            </div>
          </section>

          <section className="section-card">
            <p className="section-kicker">Operational Properties</p>
            <h3 className="section-title">{t("item.properties")}</h3>
            <div className="mt-6 grid grid-cols-1 gap-5 md:grid-cols-2">
              <div className="space-y-2">
                <label className="field-label">{t("item.weight")}</label>
                <input
                  className="input"
                  type="number"
                  step="0.01"
                  value={form.weight}
                  onChange={(e) => set("weight", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <label className="field-label">{t("item.volume")}</label>
                <input
                  className="input"
                  type="number"
                  step="0.01"
                  value={form.volume}
                  onChange={(e) => set("volume", e.target.value)}
                />
              </div>
              <div className="space-y-2">
                <label className="field-label">{t("item.maker")}</label>
                <input className="input" value={form.makerName} onChange={(e) => set("makerName", e.target.value)} />
              </div>
              <div className="space-y-2">
                <label className="field-label">{t("item.makerItemNo")}</label>
                <input
                  className="input"
                  value={form.makerItemNo}
                  onChange={(e) => set("makerItemNo", e.target.value)}
                />
              </div>
            </div>
          </section>

          <section className="section-card">
            <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <p className="section-kicker">Localization</p>
                <h3 className="section-title">{t("item.translations")}</h3>
              </div>
              <button
                className="btn-secondary"
                onClick={() =>
                  setForm({
                    ...form,
                    translations: [...form.translations, { locale: "en", name: "", description: "" }],
                  })
                }
              >
                <Plus size={16} /> {t("item.addTranslation")}
              </button>
            </div>

            <div className="mt-5 space-y-4">
              {form.translations.map((translation, index) => (
                <div key={index} className="rounded-[26px] border border-slate-200/80 bg-slate-50/70 p-5">
                  <div className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
                    <div className="flex items-center gap-3">
                      <span className="rounded-full bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500 ring-1 ring-inset ring-slate-200">
                        Locale {index + 1}
                      </span>
                      <span className="text-sm text-slate-500">
                        {LOCALE_OPTIONS.find((option) => option.value === translation.locale)?.description ?? translation.locale}
                      </span>
                    </div>
                    {form.translations.length > 1 && (
                      <button className="btn-ghost" onClick={() => removeTranslation(index)}>
                        <Trash2 size={16} />
                      </button>
                    )}
                  </div>

                  <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-4">
                    <SearchSelect
                      label={t("item.locale")}
                      value={translation.locale}
                      options={LOCALE_OPTIONS}
                      placeholder="언어 검색"
                      searchTitle="언어 선택"
                      onSelect={(option) => setTranslation(index, "locale", option.value)}
                    />
                    <div className="space-y-2 md:col-span-1">
                      <label className="field-label">{t("item.name")} *</label>
                      <input
                        className="input"
                        value={translation.name}
                        onChange={(event) => setTranslation(index, "name", event.target.value)}
                      />
                    </div>
                    <div className="space-y-2 md:col-span-2">
                      <label className="field-label">{t("je.description_")}</label>
                      <input
                        className="input"
                        value={translation.description}
                        onChange={(event) => setTranslation(index, "description", event.target.value)}
                      />
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </section>
        </div>

        <aside className="space-y-6">
          <div className="section-card xl:sticky xl:top-4">
            <p className="section-kicker">Master Summary</p>
            <h3 className="section-title">품목 요약</h3>
            <div className="mt-5 space-y-3">
              <div className="stat-tile">
                <div className="flex items-center gap-3">
                  <Package size={18} className="text-brand-600" />
                  <div>
                    <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Code / Group</div>
                    <div className="mt-1 text-sm font-semibold text-slate-900">
                      {form.code || "미입력"} / {form.itemGroup || "미선택"}
                    </div>
                  </div>
                </div>
              </div>
              <div className="stat-tile">
                <div className="flex items-center gap-3">
                  <Ruler size={18} className="text-brand-600" />
                  <div>
                    <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Unit / Weight</div>
                    <div className="mt-1 text-sm font-semibold text-slate-900">
                      {form.unitOfMeasure || "-"} / {form.weight || "-"} kg
                    </div>
                  </div>
                </div>
              </div>
              <div className="stat-tile">
                <div className="flex items-center gap-3">
                  <Languages size={18} className="text-brand-600" />
                  <div>
                    <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Translations</div>
                    <div className="mt-1 text-sm font-semibold text-slate-900">
                      {form.translations.length} locale(s)
                    </div>
                  </div>
                </div>
              </div>
              <div className="rounded-[24px] bg-slate-950 p-5 text-white">
                <div className="flex items-center gap-3">
                  <ShieldCheck size={18} className="text-sky-300" />
                  <div className="text-xs uppercase tracking-[0.16em] text-slate-400">Operational Flags</div>
                </div>
                <div className="mt-4 space-y-2 text-sm">
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">{t("item.qiRequired")}</span>
                    <span>{form.qualityInspectionRequired ? t("common.yes") : t("common.no")}</span>
                  </div>
                  <div className="flex items-center justify-between">
                    <span className="text-slate-400">{t("item.phantomBom")}</span>
                    <span>{form.phantomBom ? t("common.yes") : t("common.no")}</span>
                  </div>
                </div>
              </div>
            </div>

            <div className="mt-5 space-y-3">
              <label className="flex items-center gap-3 rounded-[22px] border border-slate-200 bg-slate-50/80 px-4 py-3">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  checked={form.qualityInspectionRequired}
                  onChange={(e) => set("qualityInspectionRequired", e.target.checked)}
                />
                <span className="text-sm text-slate-700">{t("item.qiRequired")}</span>
              </label>
              <label className="flex items-center gap-3 rounded-[22px] border border-slate-200 bg-slate-50/80 px-4 py-3">
                <input
                  type="checkbox"
                  className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  checked={form.phantomBom}
                  onChange={(e) => set("phantomBom", e.target.checked)}
                />
                <span className="text-sm text-slate-700">{t("item.phantomBom")}</span>
              </label>
            </div>
          </div>
        </aside>
      </div>
    </div>
  );
}
