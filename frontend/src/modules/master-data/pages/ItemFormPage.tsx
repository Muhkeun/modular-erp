import { useNavigate, useParams } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { Save, ArrowLeft, Trash2 } from "lucide-react";
import PageHeader from "../../../shared/components/PageHeader";
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
  code: "", itemType: "MATERIAL", itemGroup: "", unitOfMeasure: "EA",
  specification: "", weight: "", volume: "", makerName: "", makerItemNo: "",
  qualityInspectionRequired: false, phantomBom: false,
  translations: [{ locale: "ko", name: "", description: "" }],
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

  const set = (field: keyof ItemForm, value: unknown) =>
    setForm((prev) => ({ ...prev, [field]: value }));

  const setTranslation = (index: number, field: string, value: string) => {
    const updated = [...form.translations];
    updated[index] = { ...updated[index], [field]: value };
    setForm((prev) => ({ ...prev, translations: updated }));
  };

  return (
    <div>
      <PageHeader
        title={isEdit ? `${t("item.editItem")} — ${form.code}` : t("item.newItem")}
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
              <button className="btn-danger"><Trash2 size={16} /> {t("common.delete")}</button>
            )}
            <button className="btn-primary" onClick={() => saveMutation.mutate()}
              disabled={saveMutation.isPending}>
              <Save size={16} /> {saveMutation.isPending ? t("common.saving") : t("common.save")}
            </button>
          </>
        }
      />

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Basic Info */}
        <div className="card p-6 lg:col-span-2">
          <h3 className="text-base font-semibold text-slate-900 mb-5">{t("item.basicInfo")}</h3>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.code")} *</label>
              <input className="input font-mono" value={form.code} disabled={isEdit}
                onChange={(e) => set("code", e.target.value)} placeholder="ITEM-001" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.type")} *</label>
              <select className="input" value={form.itemType} onChange={(e) => set("itemType", e.target.value)}>
                <option value="MATERIAL">{t("item.types.MATERIAL")}</option>
                <option value="PRODUCT">{t("item.types.PRODUCT")}</option>
                <option value="SEMI_PRODUCT">{t("item.types.SEMI_PRODUCT")}</option>
                <option value="SERVICE">{t("item.types.SERVICE")}</option>
                <option value="ASSET">{t("item.types.ASSET")}</option>
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.group")}</label>
              <input className="input" value={form.itemGroup} onChange={(e) => set("itemGroup", e.target.value)} />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.uom")}</label>
              <select className="input" value={form.unitOfMeasure} onChange={(e) => set("unitOfMeasure", e.target.value)}>
                {["EA", "KG", "L", "M", "M2", "M3", "SET", "BOX", "ROLL"].map((u) => (
                  <option key={u} value={u}>{u}</option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.spec")}</label>
              <input className="input" value={form.specification} onChange={(e) => set("specification", e.target.value)} />
            </div>
          </div>
        </div>

        {/* Properties */}
        <div className="space-y-6">
          <div className="card p-6">
            <h3 className="text-base font-semibold text-slate-900 mb-5">{t("item.properties")}</h3>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.weight")}</label>
                <input className="input" type="number" step="0.01" value={form.weight}
                  onChange={(e) => set("weight", e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.volume")}</label>
                <input className="input" type="number" step="0.01" value={form.volume}
                  onChange={(e) => set("volume", e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.maker")}</label>
                <input className="input" value={form.makerName} onChange={(e) => set("makerName", e.target.value)} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.makerItemNo")}</label>
                <input className="input" value={form.makerItemNo} onChange={(e) => set("makerItemNo", e.target.value)} />
              </div>
            </div>
          </div>

          <div className="card p-6">
            <h3 className="text-base font-semibold text-slate-900 mb-5">{t("item.options")}</h3>
            <div className="space-y-4">
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  checked={form.qualityInspectionRequired}
                  onChange={(e) => set("qualityInspectionRequired", e.target.checked)} />
                <span className="text-sm text-slate-700">{t("item.qiRequired")}</span>
              </label>
              <label className="flex items-center gap-3 cursor-pointer">
                <input type="checkbox" className="h-4 w-4 rounded border-slate-300 text-brand-600 focus:ring-brand-500"
                  checked={form.phantomBom}
                  onChange={(e) => set("phantomBom", e.target.checked)} />
                <span className="text-sm text-slate-700">{t("item.phantomBom")}</span>
              </label>
            </div>
          </div>
        </div>

        {/* Translations */}
        <div className="card p-6 lg:col-span-3">
          <h3 className="text-base font-semibold text-slate-900 mb-5">{t("item.translations")}</h3>
          {form.translations.map((tr, i) => (
            <div key={i} className="grid grid-cols-1 sm:grid-cols-4 gap-4 mb-4 pb-4 border-b border-slate-100 last:border-0">
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.locale")}</label>
                <select className="input" value={tr.locale} onChange={(e) => setTranslation(i, "locale", e.target.value)}>
                  <option value="ko">Korean</option>
                  <option value="en">English</option>
                  <option value="ja">Japanese</option>
                  <option value="zh">Chinese</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("item.name")} *</label>
                <input className="input" value={tr.name} onChange={(e) => setTranslation(i, "name", e.target.value)} />
              </div>
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-slate-700 mb-1.5">{t("je.description_")}</label>
                <input className="input" value={tr.description} onChange={(e) => setTranslation(i, "description", e.target.value)} />
              </div>
            </div>
          ))}
          <button className="btn-ghost text-sm"
            onClick={() => setForm({ ...form, translations: [...form.translations, { locale: "en", name: "", description: "" }] })}>
            {t("item.addTranslation")}
          </button>
        </div>
      </div>
    </div>
  );
}
