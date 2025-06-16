import axios from './axios';

// 缓存已获取的数据
const cache = new Map<string | number, { typeName: string; value1: string }>();

/**
 * 根据ID获取CommonMeta的值
 * @param id CommonMeta的ID
 * @returns 返回对应的CommonMeta数据
 */
export async function getCommonMetaById(id: string | number): Promise<{ typeName: string; value1: string } | null> {
    if (!id) {
        console.debug('getCommonMetaById: ID为空');
        return null;
    }

    if (cache.has(id)) {
        console.debug(`getCommonMetaById: 从缓存获取到ID ${id} 的数据:`, cache.get(id));
        return cache.get(id)!;
    }

    try {
        console.debug(`getCommonMetaById: 正在从服务器获取ID ${id} 的数据...`);
        const res = await axios.get(`/api/common-meta/by-id/${id}`, {
            signal: AbortSignal.timeout(5000), // 5秒超时
            allowDuplicate: true
        });

        if (res.data?.success && res.data.data) {
            console.debug(`getCommonMetaById: 成功获取ID ${id} 的数据:`, res.data.data);
            cache.set(id, res.data.data);
            return res.data.data;
        }

        console.debug(`getCommonMetaById: 获取ID ${id} 的数据失败，服务器返回:`, res.data);
        return null;
    } catch (error: any) {
        if (axios.isCancel(error)) {
            console.debug(`getCommonMetaById: 请求已取消 [ID=${id}]`);
            return null;
        }
        console.error('获取CommonMeta数据失败:', error);
        return null;
    }
}

/**
 * 清除缓存数据
 */
export function clearCommonMetaCache(): void {
    console.debug('clearCommonMetaCache: 正在清除缓存...');
    cache.clear();
}

/**
 * 格式化健身记录中的类型和单位
 * @param record 健身记录数据
 * @returns 格式化后的记录
 */
export async function formatFitnessRecord<T extends { typeId: string | number; unitId: string | number; typeValue?: string; unitValue?: string }>(record: T): Promise<T> {
    if (!record || (record.typeValue && record.unitValue)) return record;

const [typeData, unitData] = await Promise.all([
    getCommonMetaById(record.typeId),
    getCommonMetaById(record.unitId)
]);

return {
    ...record,
    typeValue: record.typeValue || typeData?.value1 || '-',
    unitValue: record.unitValue || unitData?.value1 || '-'
};
}

/**
 * 格式化资产记录中的所有元数据
 * @param record 资产记录数据
 * @returns 格式化后的记录
 */
export async function formatAssetRecord<T extends { assetTypeId: string | number; unitId: string | number; assetLocationId: string | number } & Record<string, any>>(record: T): Promise<T> {
    if (!record) {
    console.debug('formatAssetRecord: 记录为空');
    return record;
}

console.debug('formatAssetRecord: 开始格式化记录:', record);
const [typeData, unitData, locationData] = await Promise.all([
    getCommonMetaById(record.assetTypeId),
    getCommonMetaById(record.unitId),
    getCommonMetaById(record.assetLocationId)
]);

const formattedRecord = {
    ...record,
    assetTypeValue: typeData?.value1 || '-',
    unitValue: unitData?.value1 || '-',
    locationValue: locationData?.value1 || '-'
};

console.debug('formatAssetRecord: 格式化后的记录:', formattedRecord);
return formattedRecord;
}
